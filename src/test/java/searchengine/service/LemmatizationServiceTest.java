package searchengine.service;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LemmatizationServiceTest {

    private final LuceneMorphology luceneMorph = mock(LuceneMorphology.class);
    private final LemmatizationService lemmatizationService = new LemmatizationService(luceneMorph);

    @Test
    void testGetLemmas_SingleWord() throws Exception {
        String text = "Я бегаю";
        Document doc = Parser.parse(text, "");

        when(luceneMorph.getNormalForms("бегаю")).thenReturn(List.of("бегать"));
        Map<String, Integer> expectedLemmas = new HashMap<>();
        expectedLemmas.put("бегать", 1);
        Map<String, Integer> result = lemmatizationService.getLemmas(doc);

        assertEquals(expectedLemmas, result);
    }

    @Test
    void testGetLemmas_MultipleWords() throws Exception {
        String text = "Я бегаю и работаю";
        Document doc = Parser.parse(text, "");

        when(luceneMorph.getNormalForms("бегаю")).thenReturn(List.of("бегать"));
        when(luceneMorph.getNormalForms("работаю")).thenReturn(List.of("работать"));

        Map<String, Integer> expectedLemmas = new HashMap<>();
        expectedLemmas.put("бегать", 1);
        expectedLemmas.put("работать", 1);

        Map<String, Integer> result = lemmatizationService.getLemmas(doc);
        assertEquals(expectedLemmas, result);
    }

    @Test
    void testGetLemmas_SpecialCharacters() throws Exception {
        String text = "Я, бегаю!!!";
        Document doc = Parser.parse(text, "");
        when(luceneMorph.getNormalForms("бегаю")).thenReturn(List.of("бегать"));

        Map<String, Integer> expectedLemmas = new HashMap<>();
        expectedLemmas.put("бегать", 1);

        Map<String, Integer> result = lemmatizationService.getLemmas(doc);
        assertEquals(expectedLemmas, result);
    }

    @Test
    void testGetLemmas_EmptyText() throws Exception {
        String text = "";
        Document doc = Parser.parse(text, "");

        Map<String, Integer> result = lemmatizationService.getLemmas(doc);
        assertTrue(result.isEmpty());
    }
}
