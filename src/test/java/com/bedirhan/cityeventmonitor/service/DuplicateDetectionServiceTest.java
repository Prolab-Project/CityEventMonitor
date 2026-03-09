package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.controller.CreateNewsRequest;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DuplicateDetectionServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SimilarityService similarityService;

    @InjectMocks
    private DuplicateDetectionService duplicateDetectionService;

    @Test
    void shouldReturnDuplicateIfExactUrlMatches() {
        CreateNewsRequest request = new CreateNewsRequest();
        request.setUrl("http://example.com/news1");

        News existing = new News();
        existing.setId("123");

        when(newsRepository.findByUrlsContaining("http://example.com/news1")).thenReturn(Optional.of(existing));

        Optional<News> result = duplicateDetectionService.findDuplicate(request);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("123");
        verifyNoInteractions(embeddingService, similarityService);
    }

    @Test
    void shouldCheckSimilarityIfUrlDoesNotMatch() {
        CreateNewsRequest request = new CreateNewsRequest();
        request.setUrl("http://example.com/news2");
        request.setTitle("Yangın");
        request.setContent("Fabrikada yangın");

        when(newsRepository.findByUrlsContaining("http://example.com/news2")).thenReturn(Optional.empty());

        News dbNews = new News();
        dbNews.setId("456");
        dbNews.setTitle("Fabrika Yangını");
        dbNews.setContent("Büyük yangın");
        
        when(newsRepository.findByPublishDateAfter(any(LocalDateTime.class))).thenReturn(List.of(dbNews));

        Map<CharSequence, Integer> reqVector = Map.of("yangın", 2);
        Map<CharSequence, Integer> dbVector = Map.of("yangın", 2, "büyük", 1);

        when(embeddingService.generateTermFrequencyVector("Yangın", "Fabrikada yangın")).thenReturn(reqVector);
        when(embeddingService.generateTermFrequencyVector("Fabrika Yangını", "Büyük yangın")).thenReturn(dbVector);

        // Simulate a match
        when(similarityService.calculateSimilarity(reqVector, dbVector)).thenReturn(0.95);

        Optional<News> result = duplicateDetectionService.findDuplicate(request);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("456");
    }

    @Test
    void shouldReturnEmptyIfSimilarityIsBelowThreshold() {
        CreateNewsRequest request = new CreateNewsRequest();
        request.setUrl("http://example.com/news3");
        request.setTitle("Kaza");

        when(newsRepository.findByUrlsContaining(anyString())).thenReturn(Optional.empty());

        News dbNews = new News();
        dbNews.setTitle("Yangın");

        when(newsRepository.findByPublishDateAfter(any(LocalDateTime.class))).thenReturn(List.of(dbNews));

        Map<CharSequence, Integer> reqVector = Map.of("kaza", 1);
        Map<CharSequence, Integer> dbVector = Map.of("yangın", 1);

        when(embeddingService.generateTermFrequencyVector(eq("Kaza"), any())).thenReturn(reqVector);
        when(embeddingService.generateTermFrequencyVector(eq("Yangın"), any())).thenReturn(dbVector);

        // Simulate no match
        when(similarityService.calculateSimilarity(reqVector, dbVector)).thenReturn(0.10);

        Optional<News> result = duplicateDetectionService.findDuplicate(request);

        assertThat(result).isEmpty();
    }
}
