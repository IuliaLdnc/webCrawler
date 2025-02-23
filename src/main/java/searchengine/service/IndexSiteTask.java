package searchengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

@RequiredArgsConstructor
@Slf4j
public class IndexSiteTask extends RecursiveTask<Void> {

    private final Site site;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final ForkJoinPool forkJoinPool;
    private final LemmatizationService lemmatizationService;

    @Override
    public Void compute() {
        try {
            initializeSiteIndexing();
            indexMainPage(site.getUrl());
            finalizeSiteIndexing();
        } catch (Exception e) {
            siteIndexingError("Ошибка при индексации: " + e.getMessage());
            log.info("Ошибка индексации сайта: " + site.getName());
        }
        return null;
    }

    private void indexMainPage(String url) {
        indexPage(url);

        List<String> links = fetchLinks(url);

        for (String link : links) {
            forkJoinPool.submit(new IndexSiteTask(site, siteRepository, pageRepository, indexRepository,
                    lemmaRepository, forkJoinPool, lemmatizationService));
        }

    }

    private void indexPage(String url){
        try {
            log.info("Индексация страницы" + url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com")
                    .timeout(5000)
                    .get();
            Page page = savePage(url, doc);
            try {
                processLemmas(page, doc);
            } catch (Exception e) {
                log.error("Ошибка при обработке лемм: {}", e.getMessage());
            }
            log.info("Страница успешно проиндексирована: " + url);
        } catch(HttpStatusException e) {
            log.error("Ошибка HTTP {} при индексации {}", e.getStatusCode(), url);
        } catch (IOException e) {
            log.info("Ошибка индексации сайта: " + url);
            e.printStackTrace();
        }
    }

    private List<String> fetchLinks(String url) {
        List<String> links = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(url).get();
            Elements linkElements = doc.select("a[href]");
            for (Element link : linkElements) {
                String linkUrl = link.attr("abs:href");
                links.add(linkUrl);
            }
        } catch (IOException e) {
            log.error("Ошибка при извлечении ссылок с " + url, e);
        }
        return links;
    }

    private Page savePage(String url, Document doc){
        Page page = new Page();
        page.setSite(site);
        page.setTitle(doc.title());
        page.setContent(doc.text());
        page.setCode(200);
        String baseUrl = site.getUrl();
        String relativePath = url.replaceFirst(baseUrl, "");
        page.setPath(relativePath.isEmpty() ? "/" : relativePath);


        return pageRepository.save(page);
    }

    private void processLemmas(Page page, Document doc) {
        Map<String, Integer> lemmas = lemmatizationService.getLemmas(doc);
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemmaText = entry.getKey();
            int count = entry.getValue();
            Lemma lemma = lemmaRepository.findByLemma(lemmaText)
                    .orElseGet(() -> {
                        Lemma newLemma = new Lemma();
                        newLemma.setLemma(lemmaText);
                        newLemma.setFrequency(0);
                        newLemma.setSite(site);
                        return lemmaRepository.save(newLemma);
                    });
            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaRepository.save(lemma);

            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(count);
            indexRepository.save(index);
        }
    }

    private void initializeSiteIndexing() {
        site.setStatus(SiteStatus.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private void finalizeSiteIndexing() {
        site.setStatus(SiteStatus.INDEXED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

    private void siteIndexingError(String errorMessage) {
        site.setStatus(SiteStatus.FAILED);
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }

}
