package com.bedirhan.cityeventmonitor.service;

import org.apache.commons.text.similarity.CosineSimilarity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SimilarityService {

    private static final double SIMILARITY_THRESHOLD = 0.90;
    private final CosineSimilarity cosineSimilarity = new CosineSimilarity();

    /**
     * İki metnin kelime frekans vektörleri arasındaki kosinüs benzerliğini hesaplar.
     * Skoru döndürür (0.0 ile 1.0 arası).
     */
    public double calculateSimilarity(Map<CharSequence, Integer> vectorA, Map<CharSequence, Integer> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }
        
        Double score = cosineSimilarity.cosineSimilarity(vectorA, vectorB);
        return score != null ? score : 0.0;
    }

    /**
     * İki vektör birbirine eşiğin (0.90) üstünde benziyor mu kontrol eder.
     */
    public boolean isDuplicate(Map<CharSequence, Integer> vectorA, Map<CharSequence, Integer> vectorB) {
        return calculateSimilarity(vectorA, vectorB) >= SIMILARITY_THRESHOLD;
    }
}
