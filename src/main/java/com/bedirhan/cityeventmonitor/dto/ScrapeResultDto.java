package com.bedirhan.cityeventmonitor.dto;

public class ScrapeResultDto {
    private int totalScraped;
    private int newSaved;
    private int duplicatesMerged;

    public ScrapeResultDto() {}

    public ScrapeResultDto(int totalScraped, int newSaved, int duplicatesMerged) {
        this.totalScraped = totalScraped;
        this.newSaved = newSaved;
        this.duplicatesMerged = duplicatesMerged;
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
}
