package searchengine.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.service.SiteIndexingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SiteIndexingService siteIndexingService;

    @Test
    void testGetStatistics() throws Exception {
        StatisticsResponse mockResponse = new StatisticsResponse();
        mockResponse.setResult(true);

        when(siteIndexingService.getStatistics()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.statistics").doesNotExist());
    }


    @Test
    void testStartIndexing_Success() throws Exception {
        when(siteIndexingService.startIndexing()).thenReturn(true);

        mockMvc.perform(get("/api/startIndexing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));
    }

    @Test
    void testStartIndexing_Failure() throws Exception {
        when(siteIndexingService.startIndexing()).thenReturn(false);

        mockMvc.perform(get("/api/startIndexing"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.error").value("Индексация уже запущена"));
    }

    @Test
    void testStopIndexing_Success() throws Exception {
        when(siteIndexingService.stopIndexing()).thenReturn(true);

        mockMvc.perform(get("/api/stopIndexing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));
    }

    @Test
    void testStopIndexing_Failure() throws Exception {
        when(siteIndexingService.stopIndexing()).thenReturn(false);

        mockMvc.perform(get("/api/stopIndexing"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.error").value("Индексация не запущенна"));
    }

    @Test
    void testIndexPage_Success() throws Exception {
        when(siteIndexingService.indexSinglePage("https://example.com")).thenReturn(true);

        mockMvc.perform(post("/api/indexPage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true));
    }

    @Test
    void testIndexPage_Failure() throws Exception {
        when(siteIndexingService.indexSinglePage("https://example.com")).thenReturn(false);

        mockMvc.perform(post("/api/indexPage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.error").value("Данная страница находится за пределами сайтов, указанными в конфигурационном файле"));
    }

    @Test
    void testIndexPage_EmptyUrl() throws Exception {
        mockMvc.perform(post("/api/indexPage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.error").value("URL не указан"));
    }

    @Test
    void testSearch_Success() throws Exception {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("result", true);
        mockResponse.put("data", List.of());

        when(siteIndexingService.search("test", null, 0, 20)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/search")
                        .param("query", "test")
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }
}

