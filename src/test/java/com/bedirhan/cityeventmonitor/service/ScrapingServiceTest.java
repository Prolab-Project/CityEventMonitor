package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.controller.CreateNewsRequest;
import com.bedirhan.cityeventmonitor.dto.ScrapeResultDto;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.model.RawNews;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScrapingServiceTest {

    private NewsScraper mockScraper;
    private TextPreprocessor mockPreprocessor;
    private NewsTypeClassifier mockClassifier;
    private LocationExtractor mockExtractor;
    private NewsService mockNewsService;

    private ScrapingService scrapingService;

    @BeforeEach
    void setUp() {
        mockScraper = mock(NewsScraper.class);
        when(mockScraper.getSourceName()).thenReturn("TestScraper");

        mockPreprocessor = mock(TextPreprocessor.class);
        mockClassifier = mock(NewsTypeClassifier.class);
        mockExtractor = mock(LocationExtractor.class);
        mockNewsService = mock(NewsService.class);

        scrapingService = new ScrapingService(
                List.of(mockScraper),
                mockPreprocessor,
                mockClassifier,
                mockExtractor,
                mockNewsService
        );
    }

    @Test
    void shouldRunPipelineAndSaveNewNews() {
        RawNews raw = new RawNews();
        raw.setTitle("Test Haber");
        raw.setContent("Test Icerik");
        raw.setUrl("http://test.com/1");
        raw.setSourceName("TestScraper");
        raw.setRawDate("bugün");

        when(mockScraper.scrape(anyInt())).thenReturn(List.of(raw));
        when(mockPreprocessor.preprocess(anyString())).thenAnswer(i -> "clean " + i.getArgument(0));
        when(mockClassifier.classify(anyString())).thenReturn(NewsType.TRAFIK_KAZASI);
        
        LocationResult loc = new LocationResult("Izmit", "merkez");
        when(mockExtractor.extract(anyString())).thenReturn(loc);

        News savedNews = new News();
        savedNews.getSources().add("TestScraper"); // new item (1 source)
        when(mockNewsService.saveAndEnrichNews(any(CreateNewsRequest.class))).thenReturn(savedNews);

        ScrapeResultDto result = scrapingService.scrapeAllSources(3);

        assertThat(result.getTotalScraped()).isEqualTo(1);
        assertThat(result.getNewSaved()).isEqualTo(1);
        assertThat(result.getDuplicatesMerged()).isEqualTo(0);

        ArgumentCaptor<CreateNewsRequest> captor = ArgumentCaptor.forClass(CreateNewsRequest.class);
        verify(mockNewsService).saveAndEnrichNews(captor.capture());

        CreateNewsRequest request = captor.getValue();
        assertThat(request.getTitle()).isEqualTo("Test Haber");
        assertThat(request.getType()).isEqualTo(NewsType.TRAFIK_KAZASI);
        assertThat(request.getDistrict()).isEqualTo("Izmit");
        assertThat(request.getLocationText()).isEqualTo("merkez");
    }

    @Test
    void shouldIdentifyDuplicatesBySourceCount() {
        RawNews raw = new RawNews();
        
        when(mockScraper.scrape(anyInt())).thenReturn(List.of(raw));
        when(mockPreprocessor.preprocess(any())).thenReturn("clean");
        when(mockClassifier.classify(any())).thenReturn(NewsType.TRAFIK_KAZASI);

        News mergedNews = new News();
        mergedNews.setSources(Set.of("Scraper1", "Scraper2")); // size > 1 means it was merged
        when(mockNewsService.saveAndEnrichNews(any())).thenReturn(mergedNews);

        ScrapeResultDto result = scrapingService.scrapeAllSources(3);

        assertThat(result.getTotalScraped()).isEqualTo(1);
        assertThat(result.getNewSaved()).isEqualTo(0);
        assertThat(result.getDuplicatesMerged()).isEqualTo(1);
    }
}
