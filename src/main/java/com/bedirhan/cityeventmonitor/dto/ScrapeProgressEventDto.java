package com.bedirhan.cityeventmonitor.dto;

/**
 * SSE ile frontend'e gönderilen sıralı scrape olayları.
 */
public class ScrapeProgressEventDto {

    /** SOURCE_START | SOURCE_DONE | COMPLETE */
    private String phase;
    private String sourceName;
    private Integer sourceIndex;
    private Integer sourceTotal;
    /** SOURCE_DONE için ham haber sayısı */
    private Integer extractedCount;
    /** COMPLETE için özet */
    private ScrapeResultDto summary;

    public static ScrapeProgressEventDto sourceStart(String sourceName, int sourceIndex, int sourceTotal) {
        ScrapeProgressEventDto e = new ScrapeProgressEventDto();
        e.phase = "SOURCE_START";
        e.sourceName = sourceName;
        e.sourceIndex = sourceIndex;
        e.sourceTotal = sourceTotal;
        return e;
    }

    public static ScrapeProgressEventDto sourceDone(String sourceName, int sourceIndex, int sourceTotal, int extractedCount) {
        ScrapeProgressEventDto e = new ScrapeProgressEventDto();
        e.phase = "SOURCE_DONE";
        e.sourceName = sourceName;
        e.sourceIndex = sourceIndex;
        e.sourceTotal = sourceTotal;
        e.extractedCount = extractedCount;
        return e;
    }

    public static ScrapeProgressEventDto complete(ScrapeResultDto summary) {
        ScrapeProgressEventDto e = new ScrapeProgressEventDto();
        e.phase = "COMPLETE";
        e.summary = summary;
        return e;
    }

    public String getPhase() {
        return phase;
    }

    public String getSourceName() {
        return sourceName;
    }

    public Integer getSourceIndex() {
        return sourceIndex;
    }

    public Integer getSourceTotal() {
        return sourceTotal;
    }

    public Integer getExtractedCount() {
        return extractedCount;
    }

    public ScrapeResultDto getSummary() {
        return summary;
    }
}
