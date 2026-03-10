package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.dto.FilterResponse;
import com.bedirhan.cityeventmonitor.dto.NewsResponseDto;
import com.bedirhan.cityeventmonitor.dto.PagedResponse;
import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class NewsService {

    private static final Logger logger = LoggerFactory.getLogger(NewsService.class);

    private final NewsRepository newsRepository;
    private final MongoTemplate mongoTemplate;
    private final DuplicateDetectionService duplicateDetectionService;
    private final GeocodingService geocodingService;
    private final NewsMapper newsMapper;

    public NewsService(NewsRepository newsRepository, MongoTemplate mongoTemplate,
                       DuplicateDetectionService duplicateDetectionService,
                       GeocodingService geocodingService,
                       NewsMapper newsMapper) {
        this.newsRepository = newsRepository;
        this.mongoTemplate = mongoTemplate;
        this.duplicateDetectionService = duplicateDetectionService;
        this.geocodingService = geocodingService;
        this.newsMapper = newsMapper;
    }

    /**
     * Dinamik filtreleme: type, district, startDate, endDate, search (title/content içinde)
     * Gelen parametrelerden null olanlar filtreden çıkarılır.
     */
    public PagedResponse<NewsResponseDto> findFiltered(NewsType type, String district,
                                                       LocalDateTime startDate, LocalDateTime endDate,
                                                       String search,
                                                       int page, int size) {

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

        // Toplam kayıt sayısını hesapla
        long total = mongoTemplate.count(query, News.class);

        // Sayfalama ve sıralama uygula
        query.with(Sort.by(Sort.Direction.DESC, "publishDate"));
        query.skip((long) page * size);
        query.limit(size);

        List<News> newsList = mongoTemplate.find(query, News.class);

        PagedResponse<NewsResponseDto> response = new PagedResponse<>();
        response.setItems(newsMapper.toDtoList(newsList));
        response.setTotalElements(total);
        int totalPages = (int) Math.ceil(total / (double) size);
        response.setTotalPages(totalPages);
        response.setPage(page);
        response.setSize(size);

        return response;
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

    /**
     * Dışarıdan veya scraper üzerinden gelen ham News oluşturma isteğini zenginleştirir (geocoding) 
     * ve duplicate kontrolünden (merge) geçirerek kaydeder.
     * @return Kaydedilen veya güncellenen News nesnesi (Eğer kopya kaynakları birebir aynıysa opsiyonel olarak boolean isNew dönülebilir ama şimdilik sadece News yeterli).
     */
    public News saveAndEnrichNews(com.bedirhan.cityeventmonitor.controller.CreateNewsRequest request) {
        // 1) Duplicate Kontrolü
        Optional<News> duplicate = duplicateDetectionService.findDuplicate(request);
        if (duplicate.isPresent()) {
            News existing = duplicate.get();
            boolean updated = false;
            if (request.getSource() != null && !request.getSource().isBlank()) {
                if(existing.getSources().add(request.getSource())) updated = true;
            }
            if (request.getUrl() != null && !request.getUrl().isBlank()) {
                if(existing.getUrls().add(request.getUrl())) updated = true;
            }
            if (updated) {
                logger.info("Duplicate found for URL/Title. Sources/URLs updated for document id: {}", existing.getId());
                return newsRepository.save(existing);
            }
            logger.debug("Duplicate found but no new sources/URLs to attach. Ignoring.");
            return existing; // Aynı ekleme işlemi yapılmadıysa direkt dön
        }

        // 2) Yeni Kayıt Oluşturma
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
        
        // request publish date null ise default anı al
        news.setPublishDate(request.getPublishDate() != null ? request.getPublishDate() : LocalDateTime.now());

        // 3) Geocoding: locationText varsa otomatik olarak lat/lng elde et
        String locationText = request.getLocationText();
        if (locationText != null && !locationText.isBlank()) {
            com.bedirhan.cityeventmonitor.model.Coordinates coords = geocodingService.geocode(locationText);
            if (coords != null) {
                news.setLatitude(coords.getLatitude());
                news.setLongitude(coords.getLongitude());
                news.setGeocodingFailed(false);
                logger.debug("Geocoding successful for locationText: {}", locationText);
            } else {
                news.setGeocodingFailed(true);
                logger.warn("Geocoding failed for locationText: {}. Setting latitude/longitude to null.", locationText);
            }
        }

        News saved = newsRepository.save(news);
        logger.info("Successfully created new News record with id: {}", saved.getId());
        return saved;
    }
}
