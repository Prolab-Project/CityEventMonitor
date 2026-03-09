package com.bedirhan.cityeventmonitor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledScraperJobTest {

    @Mock
    private ScrapingService scrapingService;

    @InjectMocks
    private ScheduledScraperJob job;

    @Test
    void shouldTriggerScrapingService() {
        // Since defaultDays is not injected via Spring in this unit test context, it will be 0.
        job.runScrapingTask();

        verify(scrapingService, times(1)).scrapeAllSources(0);
    }
}
