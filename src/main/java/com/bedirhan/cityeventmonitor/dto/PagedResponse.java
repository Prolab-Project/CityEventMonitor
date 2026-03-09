package com.bedirhan.cityeventmonitor.dto;

import lombok.Data;

import java.util.List;

@Data
public class PagedResponse<T> {

    private List<T> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}

