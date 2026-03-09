package com.bedirhan.cityeventmonitor.repository;

import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsRepository extends MongoRepository<News, String> {

    // Sadece tarih aralığı
    List<News> findByPublishDateBetween(LocalDateTime start, LocalDateTime end);

    // Sadece type
    List<News> findByType(NewsType type);

    // Sadece district
    List<News> findByDistrict(String district);

    // type + district
    List<News> findByTypeAndDistrict(NewsType type, String district);

    // type + tarih aralığı
    List<News> findByTypeAndPublishDateBetween(NewsType type, LocalDateTime start, LocalDateTime end);

    // district + tarih aralığı
    List<News> findByDistrictAndPublishDateBetween(String district, LocalDateTime start, LocalDateTime end);

    // type + district + tarih aralığı
    List<News> findByTypeAndDistrictAndPublishDateBetween(NewsType type, String district, LocalDateTime start, LocalDateTime end);

    // Duplicate kontrolü için exact URL araması
    Optional<News> findByUrlsContaining(String url);

    // Duplicate kontrolü için son N gün içindeki haberleri çekme
    List<News> findByPublishDateAfter(LocalDateTime date);
}
