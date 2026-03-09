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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/news")
public class NewsController {

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
            @RequestParam(required = false) NewsType type,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Basit rate limiting / güvenlik: sayfa ve boyutu sınırla
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // min 1, max 100

        return newsService.findFiltered(type, district, startDate, endDate, search, safePage, safeSize);
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

    @PostMapping
    public News createNews(@RequestBody CreateNewsRequest request) {
        return newsService.saveAndEnrichNews(request);
    }

    /**
     * Manuel scraping tetikleme endpoint'i.
     * Örnek: POST /api/news/scrape?days=3
     */
    @PostMapping("/scrape")
    public ScrapeResultDto scrape(@RequestParam(defaultValue = "3") int days) {
        return scrapingService.scrapeAllSources(days);
    }
}
