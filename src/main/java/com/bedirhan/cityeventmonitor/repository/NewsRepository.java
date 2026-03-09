package com.bedirhan.cityeventmonitor.repository;

import com.bedirhan.cityeventmonitor.model.News;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsRepository extends MongoRepository<News, String> {

    List<News> findByPublishDateBetween(LocalDateTime start, LocalDateTime end);
}

