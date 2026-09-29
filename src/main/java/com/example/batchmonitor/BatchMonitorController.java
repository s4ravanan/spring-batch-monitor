package com.example.batchmonitor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.Properties;

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
        private final BatchJobService jobs;
        Api(BatchMetadataRepository repository, BatchJobService jobs) { this.repository = repository; this.jobs = jobs; }

        @GetMapping("/jobs")
        Map<String, Object> jobNames() {
            return Map.of("jobs", jobs.allJobNames(), "launchable", jobs.launchableJobNames());
        }

        @GetMapping("/jobs/{jobName}/executions")
        Map<String, Object> jobExecutions(@PathVariable String jobName,
                                          @RequestParam(defaultValue = "50") int limit) {
            return Map.of("jobName", jobName, "items", repository.executionsForJob(jobName, limit),
                    "instances", repository.instances(jobName, limit));
        }

        @PostMapping("/jobs/{jobName}/launch")
        ResponseEntity<Map<String, Object>> launch(@PathVariable String jobName,
                                                   @RequestBody(required = false) Map<String, String> parameters) {
            try {
                var properties = new Properties();
                if (parameters != null) properties.putAll(parameters);
                return ResponseEntity.ok(Map.of("executionId", jobs.launch(jobName, properties)));
            } catch (Exception e) { return operationError(e); }
        }

        @PostMapping("/jobs/{jobName}/next")
        ResponseEntity<Map<String, Object>> next(@PathVariable String jobName) {
            try { return ResponseEntity.ok(Map.of("executionId", jobs.next(jobName))); }
            catch (Exception e) { return operationError(e); }
        }

        @PostMapping("/executions/{id}/stop")
        ResponseEntity<Map<String, Object>> stop(@PathVariable long id) {
            try { return ResponseEntity.ok(Map.of("accepted", jobs.stop(id))); }
            catch (Exception e) { return operationError(e); }
        }

        @PostMapping("/executions/{id}/abandon")
        ResponseEntity<Map<String, Object>> abandon(@PathVariable long id) {
            try { jobs.abandon(id); return ResponseEntity.ok(Map.of("abandoned", true)); }
            catch (Exception e) { return operationError(e); }
        }

        @PostMapping("/executions/{id}/restart")
        ResponseEntity<Map<String, Object>> restart(@PathVariable long id) {
            try { return ResponseEntity.ok(Map.of("executionId", jobs.restart(id))); }
            catch (Exception e) { return operationError(e); }
        }

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
                result.put("parameters", repository.parameters(id));
                return ResponseEntity.ok(result);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                return ResponseEntity.notFound().build();
            }
        }

        private static ResponseEntity<Map<String, Object>> operationError(Exception e) {
            var body = Map.<String, Object>of("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return ResponseEntity.status(409).body(body);
        }

        private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    }
}
