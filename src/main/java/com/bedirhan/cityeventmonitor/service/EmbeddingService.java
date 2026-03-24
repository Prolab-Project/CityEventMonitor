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
    private static final int FALLBACK_DIM = 256;

    private final TextPreprocessor textPreprocessor;
    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public EmbeddingService(TextPreprocessor textPreprocessor,
                            WebClient.Builder webClientBuilder,
                            @Value("${embedding.api-key:}") String apiKey,
                            @Value("${embedding.model:sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2}") String model,
                            @Value("${embedding.base-url:https://api-inference.huggingface.co/models}") String baseUrl) {
        this.textPreprocessor = textPreprocessor;
        this.apiKey = apiKey;
        this.model = model;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Başlık + içerikten embedding üretir.
     * Öncelik: Harici embedding modeli (HuggingFace Inference API).
     * API key yoksa/başarısızsa fallback hash-embedding döner (sistem akışı bozulmasın diye).
     */
    public List<Double> generateEmbedding(String title, String content) {
        String combined = (title != null ? title : "") + " " + (content != null ? content : "");
        String cleanedText = textPreprocessor.preprocess(combined);
        if (cleanedText.isBlank()) {
            return List.of();
        }

        List<Double> modelVector = tryRemoteEmbedding(cleanedText);
        if (modelVector != null && !modelVector.isEmpty()) {
            return modelVector;
        }
        return fallbackHashEmbedding(cleanedText);
    }

    private List<Double> tryRemoteEmbedding(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", text);
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
            // HF bazen [float,float,...], bazen [[float,...]] dönebiliyor.
            Object first = response.get(0);
            if (first instanceof Number) {
                List<Double> vec = new ArrayList<>(response.size());
                for (Object item : response) {
                    if (item instanceof Number n) vec.add(n.doubleValue());
                }
                return vec;
            }
            if (first instanceof List<?> nested) {
                List<Double> vec = new ArrayList<>(nested.size());
                for (Object item : nested) {
                    if (item instanceof Number n) vec.add(n.doubleValue());
                }
                return vec;
            }
        } catch (Exception e) {
            log.warn("Remote embedding failed, fallback'a geçiliyor: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fallback: deterministik hash tabanlı vektör.
     * Not: Asıl hedef remote embedding; bu sadece servis erişimi olmadığında akışı kırmamak içindir.
     */
    private List<Double> fallbackHashEmbedding(String text) {
        double[] arr = new double[FALLBACK_DIM];
        String[] tokens = text.split("\\s+");
        for (String t : tokens) {
            if (t.isBlank()) continue;
            int idx = Math.abs(t.hashCode()) % FALLBACK_DIM;
            arr[idx] += 1.0;
        }
        // L2 normalize
        double norm = 0.0;
        for (double v : arr) norm += v * v;
        norm = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(FALLBACK_DIM);
        for (double v : arr) {
            result.add(norm > 0 ? v / norm : 0.0);
        }
        return result;
    }
}
