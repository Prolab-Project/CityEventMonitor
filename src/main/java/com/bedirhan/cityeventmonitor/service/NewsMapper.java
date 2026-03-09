package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.dto.NewsResponseDto;
import com.bedirhan.cityeventmonitor.model.News;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewsMapper {

    public NewsResponseDto toDto(News news) {
        NewsResponseDto dto = new NewsResponseDto();
        dto.setId(news.getId());
        dto.setTitle(news.getTitle());
        dto.setContent(news.getContent());
        dto.setType(news.getType());
        dto.setDistrict(news.getDistrict());
        dto.setLocationText(news.getLocationText());
        dto.setLatitude(news.getLatitude());
        dto.setLongitude(news.getLongitude());
        dto.setGeocodingFailed(news.isGeocodingFailed());
        dto.setSources(news.getSources());
        dto.setUrls(news.getUrls());
        dto.setPublishDate(news.getPublishDate());
        return dto;
    }

    public List<NewsResponseDto> toDtoList(List<News> newsList) {
        return newsList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}

