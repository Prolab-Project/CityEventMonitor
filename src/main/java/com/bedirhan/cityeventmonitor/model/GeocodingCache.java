package com.bedirhan.cityeventmonitor.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "geocoding_cache")
public class GeocodingCache {

    @Id
    private String id;

    @Indexed(unique = true)
    private String locationText;

    private double latitude;
    private double longitude;

    private LocalDateTime createdAt;
}
