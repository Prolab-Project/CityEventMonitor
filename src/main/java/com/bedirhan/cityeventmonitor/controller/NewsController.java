package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.dto.FilterResponse;
import com.bedirhan.cityeventmonitor.dto.NewsResponseDto;
import com.bedirhan.cityeventmonitor.dto.PagedResponse;
import com.bedirhan.cityeventmonitor.dto.ScrapeResultDto;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;
    private final ScrapingService scrapingService;
    private final NewsTypeClassifier newsTypeClassifier;
    private final SemanticTypeValidator semanticTypeValidator;
    private final LocationExtractor locationExtractor;
    private final TextPreprocessor textPreprocessor;

    public NewsController(NewsService newsService, ScrapingService scrapingService,
                          NewsTypeClassifier newsTypeClassifier,
                          SemanticTypeValidator semanticTypeValidator,
                          LocationExtractor locationExtractor,
                          TextPreprocessor textPreprocessor) {
        this.newsService = newsService;
        this.scrapingService = scrapingService;
        this.newsTypeClassifier = newsTypeClassifier;
        this.semanticTypeValidator = semanticTypeValidator;
        this.locationExtractor = locationExtractor;
        this.textPreprocessor = textPreprocessor;
    }

    /**
     * Filtreli listeleme endpoint'i.
     * Tüm parametreler opsiyonel — hiçbiri verilmezse tüm haberler döner.
     *
     * GET /api/news?type=YANGIN&district=Kadıköy&startDate=2026-01-01T00:00:00&endDate=2026-03-01T00:00:00&search=patlama
     */
    @GetMapping
    public PagedResponse<NewsResponseDto> getNews(
            @RequestParam(value = "type", required = false) NewsType type,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        // Basit rate limiting / güvenlik: sayfa ve boyutu sınırla
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // min 1, max 100

        return newsService.findFiltered(type, district, startDate, endDate, search, safePage, safeSize);
    }

    /**
     * Veritabanındaki tüm haberleri sıfırlar.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAllNews() {
        long deletedCount = newsService.deleteAllNews();
        return ResponseEntity.ok(Map.of(
                "message", "Veritabanı başarıyla sıfırlandı.",
                "deletedCount", deletedCount
        ));
    }

    /**
     * Harita için sayfalamasız filtreli haber listesi.
     * GET /api/news/map?type=...&district=...&startDate=...&endDate=...&search=...
     */
    @GetMapping("/map")
    public List<NewsResponseDto> getNewsForMap(
            @RequestParam(value = "type", required = false) NewsType type,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "search", required = false) String search) {
        return newsService.findFilteredForMap(type, district, startDate, endDate, search);
    }

    /**
     * Filtre seçenekleri endpoint'i.
     * Frontend filtre dropdown'ları ve tarih aralığı için kullanır.
     *
     * GET /api/news/filters
     */
    @GetMapping("/filters")
    public FilterResponse getFilters() {
        return newsService.getFilterMetadata();
    }

    /**
     * Manuel haber oluşturma. Konum metni verilip geocoding başarısız olursa 422 döner.
     */
    @PostMapping
    public ResponseEntity<News> createNews(@RequestBody CreateNewsRequest request) {
        News news = newsService.saveAndEnrichNews(request);
        if (news == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
        }
        return ResponseEntity.ok(news);
    }

    /**
     * Manuel scraping tetikleme endpoint'i.
     * Örnek: POST /api/news/scrape?days=3
     */
    @PostMapping("/scrape")
    public ScrapeResultDto scrape(@RequestParam(value = "days", defaultValue = "3") int days) {
        return scrapingService.scrapeAllSources(days);
    }

    /**
     * Sıralı scrape ilerlemesini SSE ile yayınlar (kaynak başında/bitişinde olay).
     * GET /api/news/scrape/stream?days=3 — EventSource ile dinlenir.
     *
     * Heartbeat: Scraping uzun sürebileceğinden (5 kaynak × yüzlerce detay sayfası),
     * her 10 saniyede bir SSE comment gönderilir; böylece proxy/tarayıcı
     * bağlantıyı "idle" kabul edip kesmez.
     */
    @GetMapping(value = "/scrape/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter scrapeStream(@RequestParam(value = "days", defaultValue = "3") int days) {
        SseEmitter emitter = new SseEmitter(0L); // Sınırsız süre (Timeout yok)

        // Heartbeat: bağlantıyı canlı tutmak için periyodik SSE comment gönderir
        java.util.concurrent.ScheduledExecutorService heartbeat =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception ignored) {
                // bağlantı zaten kapanmışsa sessizce geç
                heartbeat.shutdown();
            }
        }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);

        // Temizlik: emitter herhangi bir nedenle biterse heartbeat'i durdur
        emitter.onCompletion(heartbeat::shutdown);
        emitter.onTimeout(heartbeat::shutdown);
        emitter.onError(e -> heartbeat.shutdown());

        CompletableFuture.runAsync(() -> {
            try {
                scrapingService.scrapeAllSources(days, event -> {
                    try {
                        emitter.send(SseEmitter.event().name("scrape").data(event, MediaType.APPLICATION_JSON));
                    } catch (IOException | IllegalStateException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("scrape_error").data(
                            Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Bilinmeyen hata")));
                } catch (Exception ignored) {
                    // bağlantı kapalı olabilir
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    /**
     * Sadece belirli bir kaynaktan scraping tetikleme endpoint'i.
     * Örnek: POST /api/news/scrape/source/Özgür Kocaeli?days=3
     */
    @PostMapping("/scrape/source/{sourceName}")
    public ScrapeResultDto scrapeSource(@PathVariable String sourceName, @RequestParam(value = "days", defaultValue = "3") int days) {
        return scrapingService.scrapeSource(sourceName, days);
    }

    /**
     * Sadece belirli bir kaynaktan sıralı scrape ilerlemesini SSE ile yayınlar.
     * GET /api/news/scrape/source/Özgür Kocaeli/stream?days=3
     */
    @GetMapping(value = "/scrape/source/{sourceName}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter scrapeSourceStream(@PathVariable String sourceName, @RequestParam(value = "days", defaultValue = "3") int days) {
        SseEmitter emitter = new SseEmitter(0L);

        java.util.concurrent.ScheduledExecutorService heartbeat =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        heartbeat.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception ignored) {
                heartbeat.shutdown();
            }
        }, 10, 10, java.util.concurrent.TimeUnit.SECONDS);

        emitter.onCompletion(heartbeat::shutdown);
        emitter.onTimeout(heartbeat::shutdown);
        emitter.onError(e -> heartbeat.shutdown());

        CompletableFuture.runAsync(() -> {
            try {
                scrapingService.scrapeSource(sourceName, days, event -> {
                    try {
                        emitter.send(SseEmitter.event().name("scrape").data(event, MediaType.APPLICATION_JSON));
                    } catch (IOException | IllegalStateException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("scrape_error").data(
                            Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Bilinmeyen hata")));
                } catch (Exception ignored) {}
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    /**
     * Veritabanındaki tüm haberleri yeniden işler (tür, ilçe, geocoding).
     * Sınıflandırıcı/konum çıkarıcı güncellendikten sonra mevcut kayıtları güncellemek için kullanılır.
     * POST /api/news/reprocess
     */
    @PostMapping("/reprocess")
    public java.util.Map<String, Integer> reprocess() {
        int updated = newsService.reprocessAllNews();
        return java.util.Map.of("updated", updated);
    }

    /**
     * Sınıflandırma ve konum çıkarma test endpoint'i.
     * Scrape yapmadan herhangi bir metni pipeline'dan geçirir ve sonucu döner.
     *
     * Kullanım:
     *   POST /api/news/classify
     *   Body: { "text": "Gebze'de trafik kazası: 3 araç çarpıştı" }
     *
     * Yanıt:
     *   { "type": "TRAFIK_KAZASI", "district": "Gebze", "locationText": "Gebze" }
     */
    @PostMapping("/classify")
    public Map<String, Object> classifyText(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");
        String cleaned = textPreprocessor.preprocess(text);

        NewsType keywordType = newsTypeClassifier.classify(cleaned);
        NewsType semanticType = semanticTypeValidator.validate(cleaned, "", keywordType);
        LocationResult location = locationExtractor.extract(cleaned);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("originalText", text);
        result.put("cleanedText", cleaned);
        result.put("keywordType", keywordType != null ? keywordType.name() : null);
        result.put("type", semanticType != null ? semanticType.name() : null);
        result.put("district", location != null ? location.getDistrict() : null);
        result.put("locationText", location != null ? location.getLocationText() : null);
        return result;
    }

    /**
     * URL'den haber çekip sınıflandırma test endpoint'i.
     * Verilen URL'ye gider, başlık ve içeriği çıkarır, pipeline'dan geçirir.
     *
     * Kullanım:
     *   POST /api/news/classify-url
     *   Body: { "url": "https://yenikocaeli.com/haber/..." }
     */
    @PostMapping("/classify-url")
    public Map<String, Object> classifyFromUrl(@RequestBody Map<String, String> body) {
        String url = body.getOrDefault("url", "");
        if (url.isBlank()) {
            return Map.of("error", "URL boş olamaz");
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("url", url);

        try {
            org.jsoup.nodes.Document doc = com.bedirhan.cityeventmonitor.scraper.ScraperHttp.connect(url).get();

            // Başlık: önce og:title, sonra <title>, sonra h1
            String title = null;
            org.jsoup.nodes.Element ogTitle = doc.selectFirst("meta[property=og:title]");
            if (ogTitle != null) title = ogTitle.attr("content");
            if (title == null || title.isBlank()) title = doc.title();
            if (title == null || title.isBlank()) {
                org.jsoup.nodes.Element h1 = doc.selectFirst("h1");
                if (h1 != null) title = h1.text();
            }

            String content = com.bedirhan.cityeventmonitor.scraper.DetailPageHelper.extractContent(doc);
            String dateStr = com.bedirhan.cityeventmonitor.scraper.DetailPageHelper.extractDate(doc);

            result.put("extractedTitle", title);
            result.put("extractedDate", dateStr);
            result.put("contentLength", content != null ? content.length() : 0);
            result.put("contentPreview", content != null ? content.substring(0, Math.min(300, content.length())) + "..." : null);

            // Pipeline: preprocess → classify → extract location
            String combined = (title != null ? title : "") + " " + (content != null ? content : "");
            String cleaned = textPreprocessor.preprocess(combined);

            NewsType keywordType = newsTypeClassifier.classify(cleaned);
            NewsType semanticType = semanticTypeValidator.validate(title, content, keywordType);
            LocationResult location = locationExtractor.extract(cleaned);

            result.put("keywordType", keywordType != null ? keywordType.name() : null);
            result.put("type", semanticType != null ? semanticType.name() : null);
            result.put("district", location != null ? location.getDistrict() : null);
            result.put("locationText", location != null ? location.getLocationText() : null);

        } catch (Exception e) {
            result.put("error", "Sayfa okunamadı: " + e.getMessage());
        }

        return result;
    }
}
