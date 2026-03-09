package com.bedirhan.cityeventmonitor.scraper;

import com.bedirhan.cityeventmonitor.model.RawNews;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class YeniKocaeliScraper implements NewsScraper {

    @Override
    public List<RawNews> scrapeLastDays(int days) {
        // TODO: Yeni Kocaeli için gerçek scraping implementasyonu eklenecek.
        return Collections.emptyList();
    }

    @Override
    public String getSourceName() {
        return "Yeni Kocaeli";
    }
}

