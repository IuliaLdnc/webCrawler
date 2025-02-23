package searchengine.dto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import searchengine.model.Page;

@Getter
@Setter
@AllArgsConstructor
public class SearchResult {
    private Page page;
    private double relevance;
}

