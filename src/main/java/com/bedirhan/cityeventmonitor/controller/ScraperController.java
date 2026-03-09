package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.dto.ScrapeResultDto;
import com.bedirhan.cityeventmonitor.service.ScrapingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
public class ScraperController {

    private final ScrapingService scrapingService;

    public ScraperController(ScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    /**
     * Manuel scraping tetikleme endpoint'i.
     * POST /api/scrape?days=3
     *
     * @param days Kaç günlük verinin taranacağı (varsayılan: 3)
     * @return Toplam kaç veri parse edildi, kaç tanesi veritabanına eklendi, kaç tanesi mevcut kopya olarak merge edildi.
     */
    @PostMapping
    public ScrapeResultDto triggerScrape(@RequestParam(required = false, defaultValue = "3") int days) {
        return scrapingService.scrapeAllSources(days);
    }
}
