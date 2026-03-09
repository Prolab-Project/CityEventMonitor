package com.bedirhan.cityeventmonitor.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Document(collection = "news")
public class News {

    @Id
    private String id;

    private String title;
    private String content;
    private NewsType type;

    private String locationText;

    private String district;

    private double latitude;
    private double longitude;

    private boolean geocodingFailed;

    private Set<String> sources = new HashSet<>();
    private Set<String> urls = new HashSet<>();

    private LocalDateTime publishDate;
}