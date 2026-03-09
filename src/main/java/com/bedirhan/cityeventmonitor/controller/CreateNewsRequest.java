package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.model.NewsType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateNewsRequest {

    private String title;
    private String content;
    private NewsType type;
    private String locationText;
    private String district;
    private double latitude;
    private double longitude;
    private String source;
    private String url;
    private LocalDateTime publishDate;
}

