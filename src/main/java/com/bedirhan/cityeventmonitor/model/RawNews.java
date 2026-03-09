package com.bedirhan.cityeventmonitor.model;

import lombok.Data;

/**
 * Scraping sonrasında, veritabanına kaydedilmeden önce kullanılan ham haber modeli.
 */
@Data
public class RawNews {

    private String title;
    private String content;
    private String sourceName;
    private String url;
    private String rawDate;
    private String rawLocationText;
}

