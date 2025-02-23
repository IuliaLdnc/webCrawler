package searchengine.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
class SiteIndexingServiceTest {

    @MockBean
    private SiteRepository siteRepository;

    @MockBean
    private PageRepository pageRepository;

    @MockBean
    private IndexRepository indexRepository;

    @MockBean
    private LemmaRepository lemmaRepository;

    @MockBean
    private LemmatizationService lemmatizationService;

    @Autowired
    private SiteIndexingService siteIndexingService;

    @BeforeEach
    void setUp() {
        when(siteRepository.findAll()).thenReturn(Collections.emptyList());
        when(pageRepository.count()).thenReturn(0L);
        when(lemmaRepository.count()).thenReturn(0L);
    }

    @Test
    void testStartIndexing_WhenAlreadyInProgress_ReturnsFalse() {
        siteIndexingService.startIndexing();
        boolean result = siteIndexingService.startIndexing();
        assertFalse(result, "Индексация уже запущена, должно вернуть false");
    }

    @Test
    void testStartIndexing_WhenNotInProgress_ReturnsTrue() {
        boolean result = siteIndexingService.startIndexing();
        assertTrue(result, "Индексация не была запущена, должно вернуть true");
    }


    @Test
    void testStopIndexing_WhenInProgress_ReturnsTrue() {
        siteIndexingService.startIndexing();
        boolean result = siteIndexingService.stopIndexing();
        assertTrue(result, "Индексация была запущена, должно вернуть true");
    }

    @Test
    void testIndexSinglePage_WhenSiteNotFound_ReturnsFalse() {
        String url = "http://example.com/page1";
        when(siteRepository.findAll()).thenReturn(Collections.emptyList());

        boolean result = siteIndexingService.indexSinglePage(url);
        assertFalse(result, "Индексация страницы должна быть неудачной, сайт не найден");
    }

    @Test
    void testGetStatistics_ReturnsCorrectData() {
        Site site = new Site();
        site.setUrl("http://example.com");
        site.setName("Example");
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        when(siteRepository.findAll()).thenReturn(Collections.singletonList(site));
        when(pageRepository.count()).thenReturn(100L);
        when(lemmaRepository.count()).thenReturn(50L);

        StatisticsResponse response = siteIndexingService.getStatistics();

        assertTrue(response.getResult(), "Результат должен быть true");
        assertNotNull(response.getStatistics(), "Статистика не должна быть null");
        assertEquals(1, response.getStatistics().getTotal().getSites(), "Количество сайтов должно быть 1");
    }
    @Test
    void testSearch_WhenNoResults_ReturnsEmptyList() {
        Map<String, Object> result = siteIndexingService.search("nonexistent query", null, 0, 10);

        assertTrue(result.containsKey("result"));
        assertEquals(false, result.get("result"));
        assertEquals("Не удалось извлечь леммы из поискового запроса", result.get("error"));
    }

}

