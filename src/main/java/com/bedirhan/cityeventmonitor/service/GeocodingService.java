package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.Coordinates;
import com.bedirhan.cityeventmonitor.model.GeocodingCache;
import com.bedirhan.cityeventmonitor.repository.GeocodingCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final GeocodingCacheRepository cacheRepository;
    private final WebClient webClient;
    private final String apiKey;

    public GeocodingService(GeocodingCacheRepository cacheRepository,
                            WebClient.Builder webClientBuilder,
                            @Value("${geocoding.api-key:}") String apiKey,
                            @Value("${geocoding.base-url:https://maps.googleapis.com/maps/api/geocode/json}") String baseUrl) {
        this.cacheRepository = cacheRepository;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    /**
     * locationText'ten latitude/longitude bilgisi elde eder.
     * Önce cache'e bakar, yoksa Google Geocoding API'ye istek atar.
     * Başarısız olursa null döner (exception fırlatmaz).
     */
    public Coordinates geocode(String locationText) {
        if (locationText == null || locationText.isBlank()) {
            log.debug("locationText boş, geocoding atlanıyor.");
            return null;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Geocoding API key tanımlı değil. Geocoding atlanıyor.");
            return null;
        }

        // 1) Cache kontrolü
        Optional<GeocodingCache> cached = cacheRepository.findByLocationText(locationText);
        if (cached.isPresent()) {
            GeocodingCache hit = cached.get();
            log.info("Geocoding cache hit: '{}' → ({}, {})", locationText, hit.getLatitude(), hit.getLongitude());
            return new Coordinates(hit.getLatitude(), hit.getLongitude());
        }

        // 2) Google Geocoding API çağrısı
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("address", locationText)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                log.warn("Google Geocoding API boş yanıt döndü: '{}'", locationText);
                return null;
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                log.warn("Google Geocoding API başarısız: status='{}', locationText='{}'", status, locationText);
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                log.warn("Google Geocoding API sonuç döndürmedi: '{}'", locationText);
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
            @SuppressWarnings("unchecked")
            Map<String, Object> location = (Map<String, Object>) geometry.get("location");

            double lat = ((Number) location.get("lat")).doubleValue();
            double lng = ((Number) location.get("lng")).doubleValue();

            // 3) Cache'e kaydet
            GeocodingCache cacheEntry = new GeocodingCache();
            cacheEntry.setLocationText(locationText);
            cacheEntry.setLatitude(lat);
            cacheEntry.setLongitude(lng);
            cacheEntry.setCreatedAt(LocalDateTime.now());
            cacheRepository.save(cacheEntry);

            log.info("Geocoding başarılı ve cache'lendi: '{}' → ({}, {})", locationText, lat, lng);
            return new Coordinates(lat, lng);

        } catch (Exception e) {
            log.error("Geocoding sırasında hata oluştu: '{}' — {}", locationText, e.getMessage(), e);
            return null;
        }
    }
}
