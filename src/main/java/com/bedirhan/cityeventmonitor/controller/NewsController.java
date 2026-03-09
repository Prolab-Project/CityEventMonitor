package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.dto.FilterResponse;
import com.bedirhan.cityeventmonitor.model.Coordinates;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import com.bedirhan.cityeventmonitor.service.DuplicateDetectionService;
import com.bedirhan.cityeventmonitor.service.GeocodingService;
import com.bedirhan.cityeventmonitor.service.NewsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsRepository newsRepository;
    private final NewsService newsService;
    private final GeocodingService geocodingService;
    private final DuplicateDetectionService duplicateDetectionService;

    public NewsController(NewsRepository newsRepository, NewsService newsService,
                          GeocodingService geocodingService,
                          DuplicateDetectionService duplicateDetectionService) {
        this.newsRepository = newsRepository;
        this.newsService = newsService;
        this.geocodingService = geocodingService;
        this.duplicateDetectionService = duplicateDetectionService;
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
        // Duplicate Kontrolü
        Optional<News> duplicate = duplicateDetectionService.findDuplicate(request);
        if (duplicate.isPresent()) {
            News existing = duplicate.get();
            if (request.getSource() != null && !request.getSource().isBlank()) {
                existing.getSources().add(request.getSource());
            }
            if (request.getUrl() != null && !request.getUrl().isBlank()) {
                existing.getUrls().add(request.getUrl());
            }
            return newsRepository.save(existing);
        }

        // Yeni Kayıt Oluşturma
        News news = new News();
        news.setTitle(request.getTitle());
        news.setContent(request.getContent());
        news.setType(request.getType());
        news.setLocationText(request.getLocationText());
        news.setDistrict(request.getDistrict());
        
        if (request.getSource() != null && !request.getSource().isBlank()) {
            news.getSources().add(request.getSource());
        }
        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            news.getUrls().add(request.getUrl());
        }
        
        news.setPublishDate(request.getPublishDate());

        // Geocoding: locationText varsa otomatik olarak lat/lng elde et
        String locationText = request.getLocationText();
        if (locationText != null && !locationText.isBlank()) {
            Coordinates coords = geocodingService.geocode(locationText);
            if (coords != null) {
                news.setLatitude(coords.getLatitude());
                news.setLongitude(coords.getLongitude());
                news.setGeocodingFailed(false);
            } else {
                news.setGeocodingFailed(true);
            }
        }

        return newsRepository.save(news);
    }
}
