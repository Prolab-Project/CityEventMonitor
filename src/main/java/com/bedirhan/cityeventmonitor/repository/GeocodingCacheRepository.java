package com.bedirhan.cityeventmonitor.repository;

import com.bedirhan.cityeventmonitor.model.GeocodingCache;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GeocodingCacheRepository extends MongoRepository<GeocodingCache, String> {

    Optional<GeocodingCache> findByLocationText(String locationText);
}
