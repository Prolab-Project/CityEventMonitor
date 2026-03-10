package com.bedirhan.cityeventmonitor.dto;

public class ScrapeResultDto {
    private int totalScraped;
    private int newSaved;
    private int duplicatesMerged;
    private int geocodingFailed;

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
}
