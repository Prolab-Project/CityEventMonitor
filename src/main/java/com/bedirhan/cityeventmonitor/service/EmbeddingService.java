package com.bedirhan.cityeventmonitor.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmbeddingService {

    private final TextPreprocessor textPreprocessor;

    public EmbeddingService(TextPreprocessor textPreprocessor) {
        this.textPreprocessor = textPreprocessor;
    }

    /**
     * Haber başlığı ve metnini kullanarak kelime frekans haritası üretir.
     * Apache Commons Text'in CosineSimilarity sınıfı Map<CharSequence, Integer> bekler.
     *
     * @param title Haber başlığı
     * @param content Haber içeriği
     * @return Kelime frekans vektörü (Term Frequency - TF)
     */
    public Map<CharSequence, Integer> generateTermFrequencyVector(String title, String content) {
        String combined = (title != null ? title : "") + " " + (content != null ? content : "");
        
        // Mevcut TextPreprocessor ile temizle ve normalize et
        String cleanedText = textPreprocessor.preprocess(combined);

        Map<CharSequence, Integer> vector = new HashMap<>();
        if (cleanedText.isBlank()) {
            return vector;
        }

        // Kelimelere böl
        String[] words = cleanedText.split("\\s+");
        for (String word : words) {
            // Basit stop-word filtresi (çok kısa ve anlamsız bağlaçları çıkar)
            if (isStopWord(word)) {
                continue;
            }
            vector.put(word, vector.getOrDefault(word, 0) + 1);
        }

        // Başlıktaki kelimelerin ağırlığını artır (Title weight x2)
        if (title != null && !title.isBlank()) {
            String cleanedTitle = textPreprocessor.preprocess(title);
            String[] titleWords = cleanedTitle.split("\\s+");
            for (String tw : titleWords) {
                if (!isStopWord(tw)) {
                    vector.put(tw, vector.getOrDefault(tw, 0) + 1);
                }
            }
        }

        return vector;
    }

    private boolean isStopWord(String word) {
        if (word.length() <= 2) { // "ve", "de", "da", "mi" vb.
            return true;
        }
        // Temel Türkçe stop-word'ler
        return word.matches("^(için|ile|gibi|kadar|göre|üzere|olarak|isimli|olan|ilgili|sonra|önce|ancak|fakat|veya|ya da|hem|ayrıca)$");
    }
}
