package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.RawNews;
import java.util.List;

public interface NewsScraper {
    /**
     * @param lastNDays Son N günlük haberleri getir.
     * @return Kaynaktan taranan saf haber listesi.
     */
    List<RawNews> scrape(int lastNDays);

    /**
     * @return Scraper'in kaynak adını (Örn: OzKocaeli, KocaeliKoz) döner.
     */
    String getSourceName();
}
