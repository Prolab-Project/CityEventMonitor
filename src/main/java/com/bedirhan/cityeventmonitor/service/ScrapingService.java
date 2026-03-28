package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.controller.CreateNewsRequest;
import com.bedirhan.cityeventmonitor.dto.ScrapeProgressEventDto;
import com.bedirhan.cityeventmonitor.dto.ScrapeResultDto;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.RawNews;
import com.bedirhan.cityeventmonitor.scraper.NewsScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class ScrapingService {

    private static final Logger logger = LoggerFactory.getLogger(ScrapingService.class);

    private final List<NewsScraper> scrapers;
    private final TextPreprocessor textPreprocessor;
    private final NewsTypeClassifier newsTypeClassifier;
    private final LocationExtractor locationExtractor;
    private final NewsService newsService;
    private final NewsDateParser newsDateParser;

    @Value("${scraping.default-days:3}")
    private int defaultDays;

    public ScrapingService(List<NewsScraper> scrapers,
                           TextPreprocessor textPreprocessor,
                           NewsTypeClassifier newsTypeClassifier,
                           LocationExtractor locationExtractor,
                           NewsService newsService,
                           NewsDateParser newsDateParser) {
        this.scrapers = scrapers;
        this.textPreprocessor = textPreprocessor;
        this.newsTypeClassifier = newsTypeClassifier;
        this.locationExtractor = locationExtractor;
        this.newsService = newsService;
        this.newsDateParser = newsDateParser;
    }

    public ScrapeResultDto scrapeAllSources(int days) {
        return scrapeAllSources(days, null);
    }

    /**
     * @param progress null değilse her kaynak başında/bitişinde ve en sonda SSE ile iletilir
     */
    public ScrapeResultDto scrapeAllSources(int days, Consumer<ScrapeProgressEventDto> progress) {
        int totalScraped = 0;
        int newSaved = 0;
        int duplicatesMerged = 0;
        int geocodingFailedCount = 0;
        int actualDays = (days > 0) ? days : defaultDays;
        Map<String, Integer> perSource = new LinkedHashMap<>();

        int totalSources = scrapers.size();
        logger.info("Starting scrape process for last {} days. Total scrapers: {}", actualDays, totalSources);

        for (int idx = 0; idx < totalSources; idx++) {
            NewsScraper scraper = scrapers.get(idx);
            String sourceName = scraper.getSourceName();
            int sourceIndex = idx + 1;
            if (progress != null) {
                progress.accept(ScrapeProgressEventDto.sourceStart(sourceName, sourceIndex, totalSources));
            }

            int extractedThisSource = 0;
            try {
                logger.info("Running scraper: {}", sourceName);
                List<RawNews> rawNewsList = scraper.scrape(actualDays);
                extractedThisSource = rawNewsList.size();
                totalScraped += extractedThisSource;

                LocalDateTime cutoff = LocalDateTime.now().minusDays(actualDays);
                for (RawNews raw : rawNewsList) {
                    try {
                        if (!isWithinLastDays(raw, cutoff)) {
                            logger.debug("Haber son {} gün dışında, atlanıyor: {}", actualDays, raw.getTitle());
                            continue;
                        }
                        PipelineResult result = processAndSavePipeline(raw);
                        if (result.isGeocodingFailed()) {
                            geocodingFailedCount++;
                            logger.warn("Geocoding failed for news: {} from source: {}", raw.getTitle(), sourceName);
                        } else if (result.isNew()) {
                            newSaved++;
                        } else {
                            duplicatesMerged++;
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to process RawNews from {}: {}", sourceName, ex.getMessage(), ex);
                    }
                }
                logger.info("Scraper '{}' finished. Extracted {} raw items.", sourceName, rawNewsList.size());
            } catch (Exception e) {
                logger.error("Error occurred while scraping with {}: {}", sourceName, e.getMessage());
            }
            perSource.put(sourceName, extractedThisSource);
            if (progress != null) {
                progress.accept(ScrapeProgressEventDto.sourceDone(sourceName, sourceIndex, totalSources, extractedThisSource));
            }
        }

        logger.info("Scraping finished. Total: {}, New: {}, Duplicates: {}, Geocoding Failed: {}", totalScraped, newSaved, duplicatesMerged, geocodingFailedCount);
        ScrapeResultDto dto = new ScrapeResultDto();
        dto.setTotalScraped(totalScraped);
        dto.setNewSaved(newSaved);
        dto.setDuplicatesMerged(duplicatesMerged);
        dto.setGeocodingFailed(geocodingFailedCount);
        dto.setScrapedBySource(perSource);
        if (progress != null) {
            progress.accept(ScrapeProgressEventDto.complete(dto));
        }
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

        LocalDateTime publishDate = raw.getRawDate() != null ? newsDateParser.parse(raw.getRawDate()) : null;
        request.setPublishDate(publishDate != null ? publishDate : LocalDateTime.now());

        if (locationResult != null) {
            request.setDistrict(locationResult.getDistrict());
            request.setLocationText(locationResult.getLocationText());
        } else if (raw.getRawLocationText() != null && !raw.getRawLocationText().isBlank()) {
            request.setLocationText(raw.getRawLocationText());
        }

        // 5. Geocoding, Duplicate Detection ve DB Save (NewsService içinde)
        // Geocoding başarısızsa saveAndEnrichNews null döner; kayıt oluşturulmaz.
        News savedNews = newsService.saveAndEnrichNews(request);

        if (savedNews == null) {
            return new PipelineResult(false, true); // geocoding failed, not saved
        }

        boolean isNew = savedNews.getSources() != null && savedNews.getSources().size() == 1;
        return new PipelineResult(isNew, false);
    }

    /**
     * Haberin son N gün içinde olup olmadığını kontrol eder.
     * rawDate yoksa veya parse edilemezse "son N gün" varsayımı ile kabul edilir (raporda belirtilebilir).
     */
    private boolean isWithinLastDays(RawNews raw, LocalDateTime cutoff) {
        if (raw.getRawDate() == null || raw.getRawDate().isBlank()) {
            return true;
        }
        LocalDateTime parsed = newsDateParser.parse(raw.getRawDate());
        if (parsed == null) {
            return true;
        }
        return !parsed.isBefore(cutoff);
    }
}
