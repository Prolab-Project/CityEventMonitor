package com.bedirhan.cityeventmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final TextPreprocessor textPreprocessor;
    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public EmbeddingService(TextPreprocessor textPreprocessor,
                            WebClient.Builder webClientBuilder,
                            @Value("${embedding.api-key:}") String apiKey,
                            @Value("${embedding.model:sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2}") String model,
                            @Value("${embedding.base-url:https://router.huggingface.co/hf-inference/models}") String baseUrl) {
        this.textPreprocessor = textPreprocessor;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public boolean isRemoteAvailable() {
        return apiKey != null && !apiKey.trim().isBlank();
    }

    /**
     * HF Inference API'nin yeni SentenceSimilarityPipeline formatını kullanarak,
     * hedef cümle ile referans cümleler listesi arasındaki benzerlik skorlarını döner.
     */
    public List<Double> calculateSimilarities(String sourceText, List<String> referenceTexts) {
        String cleanedSource = textPreprocessor.preprocess(sourceText);
        if (cleanedSource.isBlank() || referenceTexts == null || referenceTexts.isEmpty()) {
            return List.of();
        }

        List<Double> scores = tryRemoteSimilarities(cleanedSource, referenceTexts);
        if (scores != null && !scores.isEmpty()) {
            return scores;
        }
        
        // API hata verirse tüm skorları 0 dön, böylece validator fallback yapar.
        List<Double> fallback = new ArrayList<>(referenceTexts.size());
        for (int i = 0; i < referenceTexts.size(); i++) fallback.add(0.0);
        return fallback;
    }

    private List<Double> tryRemoteSimilarities(String sourceText, List<String> referenceTexts) {
        if (!isRemoteAvailable()) {
            return null;
        }
        try {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("source_sentence", sourceText);
            inputs.put("sentences", referenceTexts);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs);
            body.put("options", Map.of("wait_for_model", true));

            @SuppressWarnings("unchecked")
            List<Object> response = webClient.post()
                    .uri("/" + model)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (response == null || response.isEmpty()) {
                return null;
            }

            List<Double> scores = new ArrayList<>(response.size());
            for (Object item : response) {
                if (item instanceof Number n) {
                    scores.add(n.doubleValue());
                } else {
                    scores.add(0.0);
                }
            }
            return scores;
        } catch (Exception e) {
            log.warn("Remote similarity calculation failed (API down?): {}", e.getMessage());
        }
        return null;
    }

    /**
     * Sadece DuplicateDetectionService tarafından yerel benzerlik ölçümü için kullanılır.
     * Hugging Face API'ye gitmez (Zaten HF vector API'si iptal oldu), metni hashleyerek sahte vektör oluşturur.
     */
    public List<Double> generateEmbedding(String title, String content) {
        String combined = (title != null ? title : "") + " " + (content != null ? content : "");
        String cleanedText = textPreprocessor.preprocess(combined);
        if (cleanedText.isBlank()) {
            return List.of();
        }
        return fallbackHashEmbedding(cleanedText);
    }

    private static final int FALLBACK_DIM = 256;

    private List<Double> fallbackHashEmbedding(String text) {
        double[] arr = new double[FALLBACK_DIM];
        String[] tokens = text.split("\\s+");
        for (String t : tokens) {
            if (t.isBlank()) continue;
            int idx = Math.abs(t.hashCode()) % FALLBACK_DIM;
            arr[idx] += 1.0;
        }
        double norm = 0.0;
        for (double v : arr) norm += v * v;
        norm = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(FALLBACK_DIM);
        for (double v : arr) {
            result.add(norm > 0 ? (v / norm) : 0.0);
        }
        return result;
    }
}
