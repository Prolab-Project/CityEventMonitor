package com.bedirhan.cityeventmonitor.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledScraperJob {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledScraperJob.class);

    private final ScrapingService scrapingService;

    @Value("${scraping.default-days:3}")
    private int defaultDays;

    public ScheduledScraperJob(ScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    /**
     * application.properties içerisindeki cron expression'a göre çalışır.
     * Varsayılan olarak her saat başı tetiklenir ("0 0 * * * *").
     */
    @Scheduled(cron = "${scraping.cron-expression}")
    public void runScrapingTask() {
        logger.info("Starting scheduled scraping job...");
        try {
            scrapingService.scrapeAllSources(defaultDays);
            logger.info("Scheduled scraping job completed successfully.");
        } catch (Exception e) {
            logger.error("Error occurred during scheduled scraping job: {}", e.getMessage());
        }
    }
}
