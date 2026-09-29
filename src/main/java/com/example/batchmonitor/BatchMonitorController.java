package com.example.batchmonitor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class BatchMonitorController {
    private final BatchMetadataRepository repository;
    public BatchMonitorController(BatchMetadataRepository repository) { this.repository = repository; }

    @GetMapping("/")
    public String dashboard() { return "dashboard"; }

    @RestController
    @RequestMapping("/api")
    static class Api {
        private final BatchMetadataRepository repository;
        Api(BatchMetadataRepository repository) { this.repository = repository; }

        @GetMapping("/executions")
        Map<String, Object> executions(@RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "50") int limit) {
            int safeLimit = Math.max(1, Math.min(limit, 200));
            return Map.of("items", repository.latestExecutions(safeLimit, blankToNull(status)),
                    "total", repository.count(blankToNull(status)));
        }

        @GetMapping("/executions/{id}")
        ResponseEntity<Map<String, Object>> execution(@PathVariable long id) {
            try {
                var result = repository.execution(id);
                result = new java.util.HashMap<>(result);
                result.put("steps", repository.steps(id));
                return ResponseEntity.ok(result);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                return ResponseEntity.notFound().build();
            }
        }

        private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    }
}
