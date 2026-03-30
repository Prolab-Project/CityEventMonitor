package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.dto.FilterResponse;
import com.bedirhan.cityeventmonitor.dto.NewsResponseDto;
import com.bedirhan.cityeventmonitor.dto.PagedResponse;
import com.bedirhan.cityeventmonitor.dto.ScrapeResultDto;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.service.NewsService;
import com.bedirhan.cityeventmonitor.service.ScrapingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private static final Logger logger = LoggerFactory.getLogger(NewsController.class);

    private final NewsService newsService;
    private final ScrapingService scrapingService;

    public NewsController(NewsService newsService, ScrapingService scrapingService) {
        this.newsService = newsService;
        this.scrapingService = scrapingService;
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
     */
    @GetMapping(value = "/scrape/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter scrapeStream(@RequestParam(value = "days", defaultValue = "3") int days) {
        SseEmitter emitter = new SseEmitter(900_000L);
        CompletableFuture.runAsync(() -> {
            AtomicBoolean canSend = new AtomicBoolean(true);
            ScheduledExecutorService scheduler = null;
            ScheduledFuture<?> pingTask = null;
            try {
                // Bazı ortamlar SSE akışı "sessiz" kaldığında bağlantıyı kesebiliyor.
                // Bu yüzden periyodik ping gönderiyoruz (frontend'de yakalanmayan event adı: ping).
                scheduler = Executors.newSingleThreadScheduledExecutor();
                pingTask = scheduler.scheduleAtFixedRate(() -> {
                    if (!canSend.get()) return;
                    try {
                        emitter.send(SseEmitter.event().name("ping").data("ping"));
                    } catch (Exception ex) {
                        canSend.set(false);
                        logger.warn("SSE ping gönderimi başarısız, durduruluyor: {}", ex.getMessage());
                    }
                }, 20, 20, TimeUnit.SECONDS);

                scrapingService.scrapeAllSources(days, event -> {
                    if (!canSend.get()) return;
                    try {
                        emitter.send(SseEmitter.event().name("scrape").data(event, MediaType.APPLICATION_JSON));
                    } catch (IOException | IllegalStateException ex) {
                        // İstemci kopmuş olabilir; stream'i patlatmadan göndermeyi durdur.
                        canSend.set(false);
                        logger.warn("SSE gönderimi başarısız, stream durduruluyor: {}", ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("scrape_error").data(
                            Map.of("message", ex.getMessage() != null ? ex.getMessage() : "Bilinmeyen hata")));
                } catch (Exception ignored) {
                    // bağlantı kapalı olabilir
                }
                emitter.completeWithError(ex);
            } finally {
                if (pingTask != null) {
                    try {
                        pingTask.cancel(true);
                    } catch (Exception ignored) {
                    }
                }
                if (scheduler != null) {
                    try {
                        scheduler.shutdownNow();
                    } catch (Exception ignored) {
                    }
                }
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                    // complete() çoktan tamamlanmışsa ignore
                }
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
}
