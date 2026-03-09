package com.bedirhan.cityeventmonitor.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

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

    private String source;
    private String url;

    private LocalDateTime publishDate;
}