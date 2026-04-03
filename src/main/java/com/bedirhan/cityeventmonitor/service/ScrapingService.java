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
    private final SemanticTypeValidator semanticTypeValidator;
    private final LocationExtractor locationExtractor;
    private final NewsService newsService;
    private final NewsDateParser newsDateParser;

    @Value("${scraping.default-days:3}")
    private int defaultDays;

    public ScrapingService(List<NewsScraper> scrapers,
                           TextPreprocessor textPreprocessor,
                           NewsTypeClassifier newsTypeClassifier,
                           SemanticTypeValidator semanticTypeValidator,
                           LocationExtractor locationExtractor,
                           NewsService newsService,
                           NewsDateParser newsDateParser) {
        this.scrapers = scrapers;
        this.textPreprocessor = textPreprocessor;
        this.newsTypeClassifier = newsTypeClassifier;
        this.semanticTypeValidator = semanticTypeValidator;
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
        java.util.concurrent.atomic.AtomicInteger totalScraped = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger newSaved = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger duplicatesMerged = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger geocodingFailedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        int actualDays = (days > 0) ? days : defaultDays;
        Map<String, Integer> perSource = new java.util.concurrent.ConcurrentHashMap<>();

        int totalSources = scrapers.size();
        logger.info("Starting scrape process for last {} days. Total scrapers: {}", actualDays, totalSources);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(totalSources > 0 ? totalSources : 1);
        List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (int idx = 0; idx < totalSources; idx++) {
            NewsScraper scraper = scrapers.get(idx);
            final String sourceName = scraper.getSourceName();
            final int sourceIndex = idx + 1;
            
            if (progress != null) {
                synchronized (progress) {
                    progress.accept(ScrapeProgressEventDto.sourceStart(sourceName, sourceIndex, totalSources));
                }
            }

            java.util.concurrent.CompletableFuture<Void> future = java.util.concurrent.CompletableFuture.runAsync(() -> {
                int extractedThisSource = 0;
                int localNewSaved = 0;
                int localDuplicatesMerged = 0;
                int localGeocodingFailedCount = 0;
                try {
                    logger.info("Running scraper: {}", sourceName);
                    List<RawNews> rawNewsList = scraper.scrape(actualDays);
                    extractedThisSource = rawNewsList.size();

                    LocalDateTime cutoff = LocalDateTime.now().minusDays(actualDays);
                    for (RawNews raw : rawNewsList) {
                        try {
                            if (!isWithinLastDays(raw, cutoff)) {
                                logger.debug("Haber son {} gün dışında, atlanıyor: {}", actualDays, raw.getTitle());
                                continue;
                            }
                            PipelineResult result = processAndSavePipeline(raw);
                            if (result.isGeocodingFailed()) {
                                localGeocodingFailedCount++;
                                logger.warn("Geocoding failed for news: {} from source: {}", raw.getTitle(), sourceName);
                            } else if (result.isNew()) {
                                localNewSaved++;
                            } else {
                                localDuplicatesMerged++;
                            }
                        } catch (Exception ex) {
                            logger.error("Failed to process RawNews from {}: {}", sourceName, ex.getMessage(), ex);
                        }
                    }
                    logger.info("Scraper '{}' finished. Extracted {} raw items.", sourceName, rawNewsList.size());
                } catch (Exception e) {
                    logger.error("Error occurred while scraping with {}: {}", sourceName, e.getMessage());
                } finally {
                    totalScraped.addAndGet(extractedThisSource);
                    newSaved.addAndGet(localNewSaved);
                    duplicatesMerged.addAndGet(localDuplicatesMerged);
                    geocodingFailedCount.addAndGet(localGeocodingFailedCount);
                    perSource.put(sourceName, extractedThisSource);
                    
                    if (progress != null) {
                        synchronized (progress) {
                            progress.accept(ScrapeProgressEventDto.sourceDone(sourceName, sourceIndex, totalSources, extractedThisSource));
                        }
                    }
                }
            }, executor);
            futures.add(future);
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        executor.shutdown();

        logger.info("Scraping finished. Total: {}, New: {}, Duplicates: {}, Geocoding Failed: {}", totalScraped.get(), newSaved.get(), duplicatesMerged.get(), geocodingFailedCount.get());
        ScrapeResultDto dto = new ScrapeResultDto();
        dto.setTotalScraped(totalScraped.get());
        dto.setNewSaved(newSaved.get());
        dto.setDuplicatesMerged(duplicatesMerged.get());
        dto.setGeocodingFailed(geocodingFailedCount.get());
        dto.setScrapedBySource(perSource);
        
        if (progress != null) {
            synchronized (progress) {
                progress.accept(ScrapeProgressEventDto.complete(dto));
            }
        }
        return dto;
    }

    public ScrapeResultDto scrapeSource(String targetSourceName, int days) {
        return scrapeSource(targetSourceName, days, null);
    }

    public ScrapeResultDto scrapeSource(String targetSourceName, int days, Consumer<ScrapeProgressEventDto> progress) {
        NewsScraper targetScraper = null;
        for (NewsScraper s : scrapers) {
            if (s.getSourceName().replace(" ", "").equalsIgnoreCase(targetSourceName.replace(" ", ""))) {
                targetScraper = s;
                break;
            }
        }
        
        if (targetScraper == null) {
            throw new IllegalArgumentException("Scraper bulunamadı: " + targetSourceName);
        }

        int totalScraped = 0;
        int newSaved = 0;
        int duplicatesMerged = 0;
        int geocodingFailedCount = 0;
        int actualDays = (days > 0) ? days : defaultDays;
        Map<String, Integer> perSource = new LinkedHashMap<>();

        String sourceName = targetScraper.getSourceName();
        logger.info("Starting single scrape process for {} (last {} days)", sourceName, actualDays);

        if (progress != null) {
            progress.accept(ScrapeProgressEventDto.sourceStart(sourceName, 1, 1));
        }

        int extractedThisSource = 0;
        try {
            logger.info("Running scraper: {}", sourceName);
            List<RawNews> rawNewsList = targetScraper.scrape(actualDays);
            extractedThisSource = rawNewsList.size();
            totalScraped += extractedThisSource;

            LocalDateTime cutoff = LocalDateTime.now().minusDays(actualDays);
            for (RawNews raw : rawNewsList) {
                try {
                    if (!isWithinLastDays(raw, cutoff)) {
                        continue;
                    }
                    PipelineResult result = processAndSavePipeline(raw);
                    if (result.isGeocodingFailed()) {
                        geocodingFailedCount++;
                    } else if (result.isNew()) {
                        newSaved++;
                    } else {
                        duplicatesMerged++;
                    }
                } catch (Exception ex) {
                    logger.error("Failed to process RawNews from {}: {}", sourceName, ex.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred while scraping with {}: {}", sourceName, e.getMessage());
        }
        perSource.put(sourceName, extractedThisSource);

        if (progress != null) {
            progress.accept(ScrapeProgressEventDto.sourceDone(sourceName, 1, 1, extractedThisSource));
        }

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

        // 2. Haber tipi belirleme (Hibrid: Keyword → MiniLM doğrulama)
        //    - Önce keyword tabanlı sınıflandırma (hızlı, kurallı)
        //    - DIGER değilse: MiniLM ile anlamsal doğrulama (ilk 150 kar. gönderilir)
        //    - DIGER ise: MiniLM'e hiç gidilmez (API çağrısı ve zaman tasarrufu)
        var keywordType = newsTypeClassifier.classify(cleanTitle + " " + cleanContent);
        var newsType = semanticTypeValidator.validate(cleanTitle, cleanContent, keywordType);

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
