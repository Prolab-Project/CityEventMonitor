package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.dto.PagedResponse;
import com.bedirhan.cityeventmonitor.dto.NewsResponseDto;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NewsControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api/news";
    }

    @BeforeEach
    void cleanDb() {
        mongoTemplate.getDb().drop();
    }

    @Test
    void shouldCreateAndFetchNews() {
        // 1) POST /api/news ile örnek bir haber ekle
        String createUrl = baseUrl();

        Map<String, Object> body = Map.of(
                "title", "Integration Test Haberi",
                "content", "Integration test icerigi",
                "type", "TRAFIK_KAZASI",
                "locationText", "Izmit Yahyakaptan",
                "district", "Izmit",
                "source", "IntegrationTest",
                "url", "http://integration.test/haber/1",
                "publishDate", LocalDateTime.now().toString()
        );

        ResponseEntity<Void> postResponse = restTemplate.postForEntity(createUrl, body, Void.class);
        assertThat(postResponse.getStatusCode().is2xxSuccessful()).isTrue();

        // 2) GET /api/news ile sayfalı veri çek ve en az bir kayıt olduğunu doğrula
        String getUrl = baseUrl() + "?page=0&size=10";
        ResponseEntity<PagedResponse> getResponse = restTemplate.getForEntity(getUrl, PagedResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        PagedResponse<?> responseBody = getResponse.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}


package com.bedirhan.cityeventmonitor.controller;

import com.bedirhan.cityeventmonitor.model.News;
import com.bedirhan.cityeventmonitor.model.NewsType;
import com.bedirhan.cityeventmonitor.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NewsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;

    @BeforeEach
    void setUp() {
        newsRepository.deleteAll();

        News news1 = new News();
        news1.setTitle("Büyük Yangın");
        news1.setContent("Kadıköy'de büyük bir yangın çıktı.");
        news1.setType(NewsType.YANGIN);
        news1.setDistrict("Kadıköy");
        news1.setPublishDate(LocalDateTime.now().minusDays(2));
        newsRepository.save(news1);

        News news2 = new News();
        news2.setTitle("Trafik Kazası");
        news2.setContent("Şişli'de maddi hasarlı trafik kazası meydana geldi.");
        news2.setType(NewsType.TRAFIK_KAZASI);
        news2.setDistrict("Şişli");
        news2.setPublishDate(LocalDateTime.now().minusDays(1));
        newsRepository.save(news2);
    }

    @Test
    void getNews_shouldReturnAllWithPagination() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Trafik Kazası")) // Sorted by publishDate DESC by default
                .andExpect(jsonPath("$.items[1].title").value("Büyük Yangın"));
    }

    @Test
    void getNews_shouldFilterByType() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("type", NewsType.YANGIN.name())
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Büyük Yangın"));
    }

    @Test
    void getNews_shouldFilterByDistrict() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("district", "Şişli"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].district").value("Şişli"));
    }

    @Test
    void getNews_shouldFilterBySearchTerm() throws Exception {
        mockMvc.perform(get("/api/news")
                .param("search", "maddi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Trafik Kazası"));
    }

    @Test
    void getFilters_shouldReturnDistinctDistrictsAndTypes() throws Exception {
        mockMvc.perform(get("/api/news/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.districts").isArray())
                .andExpect(jsonPath("$.types").isArray())
                .andExpect(jsonPath("$.minPublishDate").isNotEmpty())
                .andExpect(jsonPath("$.maxPublishDate").isNotEmpty());
    }
}
