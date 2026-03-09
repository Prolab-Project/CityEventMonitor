package com.bedirhan.cityeventmonitor.scraper;

import com.bedirhan.cityeventmonitor.model.RawNews;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class BizimYakaScraper implements NewsScraper {

    @Override
    public List<RawNews> scrapeLastDays(int days) {
        // TODO: Bizim Yaka için gerçek scraping implementasyonu eklenecek.
        return Collections.emptyList();
    }

    @Override
    public String getSourceName() {
        return "Bizim Yaka";
    }
}

