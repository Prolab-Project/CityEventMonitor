package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.controller.CreateNewsRequest;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private final NewsRepository newsRepository;
    private final EmbeddingService embeddingService;
    private final SimilarityService similarityService;

    // Arama penceresi: Son 7 gün (1 hafta) içindeki haberlere bakılacak
    private static final int SEARCH_WINDOW_DAYS = 7;

    public DuplicateDetectionService(NewsRepository newsRepository,
                                     EmbeddingService embeddingService,
                                     SimilarityService similarityService) {
        this.newsRepository = newsRepository;
        this.embeddingService = embeddingService;
        this.similarityService = similarityService;
    }

    /**
     * Yeni gelen haber isteğinin var olan bir haberle eşleşip eşleşmediğini kontrol eder.
     *
     * @param request Yeni eklenecek haber bilgisi
     * @return Eğer duplicate(benzer) haber bulunursa Optional içinde News döner, aksi halde boş döner.
     */
    public Optional<News> findDuplicate(CreateNewsRequest request) {
        // 1) Hızlı kontrol (URL Bazlı)
        String requestUrl = request.getUrl();
        if (requestUrl != null && !requestUrl.isBlank()) {
            Optional<News> exactMatch = newsRepository.findByUrlsContaining(requestUrl.trim());
            if (exactMatch.isPresent()) {
                log.info("Duplicate bulundu (Exact URL match): '{}'", requestUrl);
                return exactMatch;
            }
        }

        // 2) Similarity (Benzerlik) kontrolü
        String title = request.getTitle();
        String content = request.getContent();
        
        if (title == null && content == null) {
            return Optional.empty(); // Kıyaslanacak metin yok
        }

        LocalDateTime searchAfter = LocalDateTime.now().minusDays(SEARCH_WINDOW_DAYS);
        List<News> recentNews = newsRepository.findByPublishDateAfter(searchAfter);

        if (recentNews.isEmpty()) {
            return Optional.empty(); // Karşılaştırılacak haber yok
        }

        Map<CharSequence, Integer> newVector = embeddingService.generateTermFrequencyVector(title, content);
        if (newVector.isEmpty()) {
            return Optional.empty();
        }

        for (News existingNews : recentNews) {
            Map<CharSequence, Integer> existingVector = embeddingService.generateTermFrequencyVector(existingNews.getTitle(), existingNews.getContent());
            
            double score = similarityService.calculateSimilarity(newVector, existingVector);
            if (score >= 0.90) { // SIMILARITY_THRESHOLD = 0.90
                log.info("Duplicate bulundu (Similarity >= 0.90) : Score={}, Mevcut Haber ID={}", score, existingNews.getId());
                return Optional.of(existingNews);
            }
        }

        return Optional.empty();
    }
}
