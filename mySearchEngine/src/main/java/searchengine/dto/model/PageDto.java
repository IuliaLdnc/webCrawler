package searchengine.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageDto {
    private Integer id;

    private Integer siteId;

    private String path;

    private Integer code;

    private String content;

    private String title;
}
