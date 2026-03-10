package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.controller.CreateNewsRequest;
import com.bedirhan.cityeventmonitor.dto.ScrapeResultDto;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.RawNews;
import com.bedirhan.cityeventmonitor.scraper.NewsScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(ScrapingService.class);

    private final List<NewsScraper> scrapers;
    private final TextPreprocessor textPreprocessor;
    private final NewsTypeClassifier newsTypeClassifier;
    private final LocationExtractor locationExtractor;
    private final NewsService newsService;

    @Value("${scraping.default-days:3}")
    private int defaultDays;

    public ScrapingService(List<NewsScraper> scrapers,
                           TextPreprocessor textPreprocessor,
                           NewsTypeClassifier newsTypeClassifier,
                           LocationExtractor locationExtractor,
                           NewsService newsService) {
        this.scrapers = scrapers;
        this.textPreprocessor = textPreprocessor;
        this.newsTypeClassifier = newsTypeClassifier;
        this.locationExtractor = locationExtractor;
        this.newsService = newsService;
    }

    public ScrapeResultDto scrapeAllSources(int days) {
        int totalScraped = 0;
        int newSaved = 0;
        int duplicatesMerged = 0;
        int geocodingFailedCount = 0;
        int actualDays = (days > 0) ? days : defaultDays;

        logger.info("Starting scrape process for last {} days. Total scrapers: {}", actualDays, scrapers.size());

        for (NewsScraper scraper : scrapers) {
            try {
                logger.info("Running scraper: {}", scraper.getSourceName());
                List<RawNews> rawNewsList = scraper.scrape(actualDays);
                totalScraped += rawNewsList.size();

                for (RawNews raw : rawNewsList) {
                    try {
                        PipelineResult result = processAndSavePipeline(raw);
                        if (result.isNew()) {
                            newSaved++;
                        } else {
                            duplicatesMerged++;
                        }
                        if (result.isGeocodingFailed()) {
                            geocodingFailedCount++;
                            logger.warn("Geocoding failed for news: {} from source: {}", raw.getTitle(), scraper.getSourceName());
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to process RawNews from {}: {}", scraper.getSourceName(), ex.getMessage(), ex);
                    }
                }
                logger.info("Scraper '{}' finished. Extracted {} raw items.", scraper.getSourceName(), rawNewsList.size());
            } catch (Exception e) {
                logger.error("Error occurred while scraping with {}: {}", scraper.getSourceName(), e.getMessage());
            }
        }

        logger.info("Scraping finished. Total: {}, New: {}, Duplicates: {}, Geocoding Failed: {}", totalScraped, newSaved, duplicatesMerged, geocodingFailedCount);
        ScrapeResultDto dto = new ScrapeResultDto();
        dto.setTotalScraped(totalScraped);
        dto.setNewSaved(newSaved);
        dto.setDuplicatesMerged(duplicatesMerged);
        dto.setGeocodingFailed(geocodingFailedCount);
        return dto;
    }

    record PipelineResult(boolean isNew, boolean isGeocodingFailed) {}

    /**
     * Pipeline akışı. Her RawNews nesnesini işler.
     */
    private PipelineResult processAndSavePipeline(RawNews raw) {
        // 1. Text Preprocessing
        String cleanTitle = textPreprocessor.preprocess(raw.getTitle());
        String cleanContent = textPreprocessor.preprocess(raw.getContent());

        // 2. Haber tipi belirleme
        var newsType = newsTypeClassifier.classify(cleanTitle + " " + cleanContent);

        // 3. Lokasyon çıkarma
        var locationResult = locationExtractor.extract(cleanTitle + " " + cleanContent);

        // 4. Eklenecek Obje oluşturma (CreateNewsRequest veya DTO eşleştiricisi)
        CreateNewsRequest request = new CreateNewsRequest();
        request.setTitle(raw.getTitle());
        request.setContent(raw.getContent());
        request.setType(newsType);
        request.setSource(raw.getSourceName());
        request.setUrl(raw.getUrl());
        // For now, defaulting publishDate to now since we haven't implemented Date parsing for scrapers yet
        request.setPublishDate(LocalDateTime.now());

        if (locationResult != null) {
            request.setDistrict(locationResult.getDistrict());
            request.setLocationText(locationResult.getLocationText());
        }

        // 5. Geocoding, Duplicate Detection ve DB Save (NewsService içinde)
        News savedNews = newsService.saveAndEnrichNews(request);
        
        // Bu aşamada haberin yeni mi yoksa güncellenen bir kopya mı olduğunu
        // kabaca url veya sources sayısına bakarak ya da ID durumuna bakarak anlayabiliriz.
        // saveAndEnrichNews duplicate bulduğunda setlere eleman ekler. İlerde daha net ayrım yapılabilir.
        // Şimdilik eklendiği varsayımı için isNew kontrolü: (eğer source var ama sadece 1 tane ise genelde yenidir)
        boolean isNew = savedNews.getSources() != null && savedNews.getSources().size() == 1;
        boolean isGeocodingFailed = savedNews.isGeocodingFailed();
        return new PipelineResult(isNew, isGeocodingFailed);
    }
}
