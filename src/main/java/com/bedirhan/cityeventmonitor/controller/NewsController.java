package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsRepository newsRepository;

    public NewsController(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @GetMapping
    public List<News> getAllNews() {
        return newsRepository.findAll();
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

