package com.bedirhan.cityeventmonitor.service;

import com.bedirhan.cityeventmonitor.model.Coordinates;
import com.bedirhan.cityeventmonitor.model.GeocodingCache;
import com.bedirhan.cityeventmonitor.repository.GeocodingCacheRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock
    private GeocodingCacheRepository cacheRepository;

    private MockWebServer mockWebServer;
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();

        geocodingService = new GeocodingService(cacheRepository, webClientBuilder, "test-api-key", baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnNullWhenLocationTextIsNull() {
        Coordinates result = geocodingService.geocode(null);

        assertThat(result).isNull();
        verifyNoInteractions(cacheRepository);
    }

    @Test
    void shouldReturnNullWhenLocationTextIsBlank() {
        Coordinates result = geocodingService.geocode("   ");

        assertThat(result).isNull();
        verifyNoInteractions(cacheRepository);
    }

    @Test
    void shouldReturnNullWhenApiKeyIsEmpty() {
        WebClient.Builder builder = WebClient.builder();
        GeocodingService serviceWithoutKey = new GeocodingService(cacheRepository, builder, "",
                mockWebServer.url("/").toString());

        Coordinates result = serviceWithoutKey.geocode("İzmit, Kocaeli");

        assertThat(result).isNull();
        verifyNoInteractions(cacheRepository);
    }

    @Test
    void shouldReturnFromCacheWhenEntryExists() {
        GeocodingCache cached = new GeocodingCache();
        cached.setLocationText("İzmit, Kocaeli");
        cached.setLatitude(40.7655);
        cached.setLongitude(29.9408);
        cached.setCreatedAt(LocalDateTime.now());

        when(cacheRepository.findByLocationText("İzmit, Kocaeli")).thenReturn(Optional.of(cached));

        Coordinates result = geocodingService.geocode("İzmit, Kocaeli");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(40.7655);
        assertThat(result.getLongitude()).isEqualTo(29.9408);

        // API'ye istek atılmadığını doğrula (MockWebServer'a istek gelmemeli)
        assertThat(mockWebServer.getRequestCount()).isZero();
    }

    @Test
    void shouldCallApiAndCacheResultOnSuccess() {
        when(cacheRepository.findByLocationText("Gebze, Kocaeli")).thenReturn(Optional.empty());

        String jsonResponse = """
                {
                    "status": "OK",
                    "results": [
                        {
                            "geometry": {
                                "location": {
                                    "lat": 40.8027,
                                    "lng": 29.4307
                                }
                            }
                        }
                    ]
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinates result = geocodingService.geocode("Gebze, Kocaeli");

        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isEqualTo(40.8027);
        assertThat(result.getLongitude()).isEqualTo(29.4307);

        // Cache'e kaydedildiğini doğrula
        ArgumentCaptor<GeocodingCache> captor = ArgumentCaptor.forClass(GeocodingCache.class);
        verify(cacheRepository).save(captor.capture());
        GeocodingCache saved = captor.getValue();
        assertThat(saved.getLocationText()).isEqualTo("Gebze, Kocaeli");
        assertThat(saved.getLatitude()).isEqualTo(40.8027);
        assertThat(saved.getLongitude()).isEqualTo(29.4307);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldReturnNullWhenApiReturnsZeroResults() {
        when(cacheRepository.findByLocationText("bilinmeyen konum")).thenReturn(Optional.empty());

        String jsonResponse = """
                {
                    "status": "ZERO_RESULTS",
                    "results": []
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinates result = geocodingService.geocode("bilinmeyen konum");

        assertThat(result).isNull();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void shouldReturnNullWhenApiReturnsError() {
        when(cacheRepository.findByLocationText("hatalı konum")).thenReturn(Optional.empty());

        String jsonResponse = """
                {
                    "status": "REQUEST_DENIED",
                    "results": []
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json"));

        Coordinates result = geocodingService.geocode("hatalı konum");

        assertThat(result).isNull();
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void shouldReturnNullWhenApiCallThrowsException() {
        when(cacheRepository.findByLocationText("timeout konum")).thenReturn(Optional.empty());

        // Sunucu bağlantıyı kapatarak hata simüle et
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Coordinates result = geocodingService.geocode("timeout konum");

        assertThat(result).isNull();
        verify(cacheRepository, never()).save(any());
    }
}
