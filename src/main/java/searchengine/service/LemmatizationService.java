package searchengine.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
@Slf4j
public class LemmatizationService {

    private final LuceneMorphology luceneMorph;
    private final List<String> UNIONS = List.of("и", "да", "тоже", "так же", "но", "а", "или", "либо", "зато",
            "однако", "притом", "причем", "не столько", "чтобы", "чтоб");

    public List<String> getSortedWords(Document doc) {
        String text = doc.text();

        List<String> words = new ArrayList<>(Arrays.asList(text.split("\\s+")));
        words.removeIf(UNIONS::contains);
        return words;
    }

    public Map<String, Integer> getLemmas(Document doc) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        List<String> sortedWords = getSortedWords(doc);

        for (String word : sortedWords) {
            word = word.toLowerCase().replaceAll("[^а-яА-Я]", "");
            if (word.isEmpty()) continue;

            try {
                List<String> normalForms = luceneMorph.getNormalForms(word);
                if (normalForms != null && !normalForms.isEmpty()) {
                    for (String lemma : normalForms) {
                        lemmas.put(lemma, lemmas.getOrDefault(lemma, 0) + 1);
                    }
                } else {
                    log.warn("Не удалось получить лемму для слова: {}", word);
                }
            } catch (Exception e) {
                log.error("Ошибка при обработке слова: {}", word, e);
            }
        }
        return lemmas;
    }
}

