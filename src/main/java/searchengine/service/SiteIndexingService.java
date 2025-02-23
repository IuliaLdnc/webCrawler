package searchengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.model.SearchResult;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class SiteIndexingService {

    private boolean indexingInProgress = false;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(4);
    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmatizationService lemmatizationService;

    public boolean startIndexing() {
        if (indexingInProgress) {
            log.warn("Индексация уже в процессе.");
            return false;
        }
        indexingInProgress = true;
        log.info("Индексация началась.");

        sitesList.getSites().forEach(siteFromConfig -> {
            Optional<Site> existingSite = siteRepository.findByUrl(siteFromConfig.getUrl());
            if (existingSite.isEmpty()) {
                Site site = new Site();
                site.setUrl(siteFromConfig.getUrl());
                site.setName(siteFromConfig.getName());
                site.setStatus(SiteStatus.SAVED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }
        });

        siteRepository.findAll()
                .forEach(site -> forkJoinPool.submit(
                        new IndexSiteTask(site, siteRepository, pageRepository, indexRepository,
                                lemmaRepository, forkJoinPool, lemmatizationService)));

        return true;
    }

    public boolean stopIndexing() {
        if (!indexingInProgress) {
            log.warn("Индексация не была запущена.");
            return false;
        }

        indexingInProgress = false;
        log.info("Индексация остановлена.");
        forkJoinPool.shutdownNow();

        List<Site> sites = siteRepository.findAllByStatus(SiteStatus.INDEXING);
        for (Site site : sites) {
            site.setStatus(SiteStatus.FAILED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError("Индексация остановлена пользователем");
            siteRepository.save(site);

            List<Page> pages = pageRepository.findBySiteId(site.getId());
            for (Page page : pages) {
                if (page.getCode() != 200) {
                    page.setSite(site);
                    pageRepository.save(page);
                }
            }
        }
        return true;
    }

    public boolean indexSinglePage(String url) {
        Optional<Site> siteOptional = siteRepository.findAll().stream()
                .filter(site -> url.startsWith(site.getUrl()))
                .findFirst();

        if (siteOptional.isEmpty()) {
            log.warn("Не удалось найти сайт для URL: {}", url);
            return false;
        }

        Site site = siteOptional.get();
        pageRepository.findByPath(url.replace(site.getUrl(), "")).ifPresent(existingPage -> {
            indexRepository.deleteByPage(existingPage);
            lemmaRepository.deleteUnusedLemmas();
            pageRepository.delete(existingPage);
        });

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .referrer("http://www.google.com")
                    .timeout(5000)
                    .get();

            Page page = new Page();
            page.setSite(site);
            page.setTitle(doc.title());
            page.setContent(doc.text());
            page.setCode(200);
            String relativePath = url.replaceFirst(site.getUrl(), "");
            page.setPath(relativePath.isEmpty() ? "/" : relativePath);
            page = pageRepository.save(page);

            Map<String, Integer> lemmas = lemmatizationService.getLemmas(doc);
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                Lemma lemma = lemmaRepository.findByLemma(entry.getKey())
                        .orElseGet(() -> {
                            Lemma newLemma = new Lemma();
                            newLemma.setLemma(entry.getKey());
                            newLemma.setFrequency(0);
                            newLemma.setSite(site);
                            return lemmaRepository.save(newLemma);
                        });
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);

                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(entry.getValue());
                indexRepository.save(index);
            }

            log.info("Страница успешно проиндексирована: {}", url);
        } catch (IOException e) {
            log.error("Ошибка при индексации страницы {}: {}", url, e.getMessage());
            return false;
        }

        return true;
    }

    public StatisticsResponse getStatistics() {
        List<Site> sites = siteRepository.findAll();
        int totalPages = (int) pageRepository.count();
        int totalLemmas = (int) lemmaRepository.count();
        boolean indexing = sites.stream().anyMatch(site -> site.getStatus() == SiteStatus.INDEXING);

        List<DetailedStatisticsItem> detailedStatistics = sites.stream().map(site -> {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());
            item.setStatus(site.getStatus().name());
            item.setStatusTime(site.getStatusTime().toEpochSecond(ZoneOffset.UTC));
            item.setPages(pageRepository.countBySite(site));
            item.setLemmas(lemmaRepository.countBySite(site));
            if (site.getStatus() == SiteStatus.FAILED) {
                item.setError(site.getLastError());
            }
            return item;
        }).collect(Collectors.toList());

        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites(sites.size());
        totalStatistics.setPages(totalPages);
        totalStatistics.setLemmas(totalLemmas);
        totalStatistics.setIndexing(indexing);

        StatisticsData statisticsData = new StatisticsData();
        statisticsData.setTotal(totalStatistics);
        statisticsData.setDetailed(detailedStatistics);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(statisticsData);

        return response;
    }

    public Map<String, Object> search(String query, String site, int offset, int limit) {
        Map<String, Object> response = new HashMap<>();
        Document doc = Jsoup.parse(query);
        Map<String, Integer> lemmaMap = lemmatizationService.getLemmas(doc);
        List<String> lemmas = new ArrayList<>(lemmaMap.keySet());

        if (lemmas.isEmpty()) {
            response.put("result", false);
            response.put("error", "Не удалось извлечь леммы из поискового запроса");
            return response;
        }

        long totalPages = pageRepository.count();
        double maxFrequencyThreshold = 0.8 * totalPages;

        lemmas = lemmas.stream()
                .filter(lemma -> {
                    Integer frequency = lemmaRepository.findByLemma(lemma)
                            .map(Lemma::getFrequency)
                            .orElse(Integer.MAX_VALUE);
                    return frequency < maxFrequencyThreshold;
                })
                .sorted(Comparator.comparingInt(lemma -> lemmaRepository.findByLemma(lemma)
                        .map(Lemma::getFrequency)
                        .orElse(Integer.MAX_VALUE)))
                .collect(Collectors.toList());

        if (lemmas.isEmpty()) {
            response.put("result", false);
            response.put("error", "Все найденные леммы являются слишком частыми и были исключены");
            return response;
        }

        List<Page> pages = pageRepository.findPagesByLemmas(lemmas, site, lemmas.size());

        if (pages.isEmpty()) {
            response.put("result", true);
            response.put("count", 0);
            response.put("data", new ArrayList<>());
            return response;
        }

        Map<Page, Double> relevanceMap = pages.stream()
                .collect(Collectors.toMap(page -> page, this::calculateRelevance));

        double maxRelevance = relevanceMap.values().stream()
                .max(Double::compare)
                .orElse(1.0);

        List<SearchResult> results = pages.stream()
                .map(page -> new SearchResult(page, relevanceMap.get(page) / maxRelevance))
                .sorted(Comparator.comparingDouble(SearchResult::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        response.put("result", true);
        response.put("count", pages.size());
        response.put("data", results);

        return response;
    }

    private double calculateRelevance(Page page) {
        return indexRepository.findByPage(page).stream()
                .mapToDouble(Index::getRank)
                .sum();
    }
}
