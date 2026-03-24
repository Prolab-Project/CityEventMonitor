package com.bedirhan.cityeventmonitor.service;

import org.apache.commons.text.similarity.CosineSimilarity;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /**
     * Embedding vektörleri için cosine similarity.
     */
    public double calculateEmbeddingSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }
        int dim = Math.min(vectorA.size(), vectorB.size());
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < dim; i++) {
            double a = vectorA.get(i);
            double b = vectorB.get(i);
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public boolean isDuplicateEmbeddings(List<Double> vectorA, List<Double> vectorB) {
        return calculateEmbeddingSimilarity(vectorA, vectorB) >= SIMILARITY_THRESHOLD;
    }
}
