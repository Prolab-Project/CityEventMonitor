package com.bedirhan.cityeventmonitor.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScrapeResultDto {
    private int totalScraped;
    private int newSaved;
    private int duplicatesMerged;
    private int geocodingFailed;
    /** Kaynak adı → o siteden parse edilen ham haber sayısı (sıra scraper çalışma sırası) */
    private Map<String, Integer> scrapedBySource = new LinkedHashMap<>();

    public ScrapeResultDto() {}

    public ScrapeResultDto(int totalScraped, int newSaved, int duplicatesMerged, int geocodingFailed) {
        this.totalScraped = totalScraped;
        this.newSaved = newSaved;
        this.duplicatesMerged = duplicatesMerged;
        this.geocodingFailed = geocodingFailed;
    }

    public int getTotalScraped() {
        return totalScraped;
    }

    public void setTotalScraped(int totalScraped) {
        this.totalScraped = totalScraped;
    }

    public int getNewSaved() {
        return newSaved;
    }

    public void setNewSaved(int newSaved) {
        this.newSaved = newSaved;
    }

    public int getDuplicatesMerged() {
        return duplicatesMerged;
    }

    public void setDuplicatesMerged(int duplicatesMerged) {
        this.duplicatesMerged = duplicatesMerged;
    }

    public int getGeocodingFailed() {
        return geocodingFailed;
    }

    public void setGeocodingFailed(int geocodingFailed) {
        this.geocodingFailed = geocodingFailed;
    }

    public Map<String, Integer> getScrapedBySource() {
        return scrapedBySource;
    }

    public void setScrapedBySource(Map<String, Integer> scrapedBySource) {
        this.scrapedBySource = scrapedBySource != null ? scrapedBySource : new LinkedHashMap<>();
    }
}
