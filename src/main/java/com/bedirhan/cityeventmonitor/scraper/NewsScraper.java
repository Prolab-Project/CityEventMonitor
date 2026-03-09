package com.bedirhan.cityeventmonitor.scraper;

import com.bedirhan.cityeventmonitor.model.RawNews;

import java.util.List;

public interface NewsScraper {

    /**
     * Kaynaktan son N güne ait haberleri çeker.
     *
     * @param days Kaç günlük haber çekileceği
     * @return Ham haber listesi
     */
    List<RawNews> scrapeLastDays(int days);

    /**
     * Haber kaynağının kısa adı (ör. "Çağdaş Kocaeli").
     */
    String getSourceName();
}

