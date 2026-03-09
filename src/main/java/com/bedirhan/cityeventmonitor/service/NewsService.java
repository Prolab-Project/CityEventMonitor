package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.dto.FilterResponse;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NewsService {

    private final NewsRepository newsRepository;
    private final MongoTemplate mongoTemplate;

    public NewsService(NewsRepository newsRepository, MongoTemplate mongoTemplate) {
        this.newsRepository = newsRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Dinamik filtreleme: type, district, startDate, endDate, search (title/content içinde)
     * Gelen parametrelerden null olanlar filtreden çıkarılır.
     */
    public List<News> findFiltered(NewsType type, String district,
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   String search) {

        Query query = new Query();

        if (type != null) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (district != null && !district.isBlank()) {
            query.addCriteria(Criteria.where("district").is(district));
        }
        if (startDate != null && endDate != null) {
            query.addCriteria(Criteria.where("publishDate").gte(startDate).lte(endDate));
        } else if (startDate != null) {
            query.addCriteria(Criteria.where("publishDate").gte(startDate));
        } else if (endDate != null) {
            query.addCriteria(Criteria.where("publishDate").lte(endDate));
        }
        if (search != null && !search.isBlank()) {
            // title veya content içinde case-insensitive arama
            Pattern regex = Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(regex),
                    Criteria.where("content").regex(regex)
            ));
        }

        query.with(Sort.by(Sort.Direction.DESC, "publishDate"));

        return mongoTemplate.find(query, News.class);
    }

    /**
     * Frontend'in filtre seçeneklerini oluşturması için:
     * - Tüm NewsType enum değerleri
     * - Mongo'dan distinct district listesi
     * - Min & max publishDate
     */
    public FilterResponse getFilterMetadata() {
        FilterResponse response = new FilterResponse();

        // 1) Tüm enum değerleri
        response.setTypes(Arrays.asList(NewsType.values()));

        // 2) Distinct district listesi
        List<String> districts = mongoTemplate.findDistinct("district", News.class, String.class);
        districts.sort(String::compareTo);
        response.setDistricts(districts);

        // 3) Min publishDate
        Query minQuery = new Query().with(Sort.by(Sort.Direction.ASC, "publishDate")).limit(1);
        News oldest = mongoTemplate.findOne(minQuery, News.class);
        if (oldest != null) {
            response.setMinPublishDate(oldest.getPublishDate());
        }

        // 4) Max publishDate
        Query maxQuery = new Query().with(Sort.by(Sort.Direction.DESC, "publishDate")).limit(1);
        News newest = mongoTemplate.findOne(maxQuery, News.class);
        if (newest != null) {
            response.setMaxPublishDate(newest.getPublishDate());
        }

        return response;
    }
}
