package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.dto.FilterResponse;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import com.bedirhan.cityeventmonitor.service.NewsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsRepository newsRepository;
    private final NewsService newsService;

    public NewsController(NewsRepository newsRepository, NewsService newsService) {
        this.newsRepository = newsRepository;
        this.newsService = newsService;
    }

    /**
     * Filtreli listeleme endpoint'i.
     * Tüm parametreler opsiyonel — hiçbiri verilmezse tüm haberler döner.
     *
     * GET /api/news?type=YANGIN&district=Kadıköy&startDate=2026-01-01T00:00:00&endDate=2026-03-01T00:00:00&search=patlama
     */
    @GetMapping
    public List<News> getNews(
            @RequestParam(required = false) NewsType type,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String search) {

        return newsService.findFiltered(type, district, startDate, endDate, search);
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
        News news = new News();
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setType(request.getType());
        news.setLocationText(request.getLocationText());
        news.setDistrict(request.getDistrict());
        news.setLatitude(request.getLatitude());
        news.setLongitude(request.getLongitude());
        news.setSource(request.getSource());
        news.setUrl(request.getUrl());
        news.setPublishDate(request.getPublishDate());
        return newsRepository.save(news);
    }
}
