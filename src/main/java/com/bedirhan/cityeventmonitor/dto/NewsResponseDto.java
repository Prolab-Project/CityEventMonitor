package com.bedirhan.cityeventmonitor.dto;

import com.bedirhan.cityeventmonitor.model.NewsType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class NewsResponseDto {

    private String id;
    private String title;
    private String content;
    private NewsType type;
    private String district;
    private String locationText;
    private double latitude;
    private double longitude;
    private boolean geocodingFailed;
    private Set<String> sources;
    private Set<String> urls;
    private LocalDateTime publishDate;
}

