package searchengine.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.service.SiteIndexingService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final SiteIndexingService siteIndexingService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
        StatisticsResponse statistics = siteIndexingService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        boolean started = siteIndexingService.startIndexing();
        HashMap<String, Object> response = new HashMap<>();
        if(!started) {
            response.put("result", false);
            response.put("error", "Индексация уже запущена");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        response.put("result", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<HashMap<String, Object>> stopIndexing() {
        HashMap<String, Object> response = new HashMap<>();
        boolean stopped = siteIndexingService.stopIndexing();
        if(!stopped) {
            response.put("result", false);
            response.put("error", "Индексация не запущенна");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        response.put("result", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        Map<String, Object> response = new HashMap<>();

        if (url == null || url.isEmpty()) {
            response.put("result", false);
            response.put("error", "URL не указан");
            return ResponseEntity.badRequest().body(response);
        }

        boolean success = siteIndexingService.indexSinglePage(url);

        if (!success) {
            response.put("result", false);
            response.put("error", "Данная страница находится за пределами сайтов, указанными в конфигурационном файле");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("result", true);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String query,
                                                      @RequestParam(required = false) String site,
                                                      @RequestParam(defaultValue = "0") int offset,
                                                      @RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> response = siteIndexingService.search(query, site, offset, limit);
        return ResponseEntity.ok(response);
    }
}
