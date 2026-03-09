package com.bedirhan.cityeventmonitor.dto;

import com.bedirhan.cityeventmonitor.model.NewsType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FilterResponse {

    private List<NewsType> types;
    private List<String> districts;
    private LocalDateTime minPublishDate;
    private LocalDateTime maxPublishDate;
}
