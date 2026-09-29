package com.example.batchmonitor;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class BatchFileController {
    private final BatchFileService files;
    public BatchFileController(BatchFileService files) { this.files = files; }

    @GetMapping
    Map<String, Object> list(@RequestParam(defaultValue = "") String path) throws Exception {
        return Map.of("path", path, "files", files.list(path));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> upload(@RequestParam(defaultValue = "") String path, @RequestPart MultipartFile file) throws Exception {
        return Map.of("path", files.upload(path, file));
    }

    @GetMapping("/content")
    ResponseEntity<Resource> download(@RequestParam String path) {
        Resource resource = files.download(path);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(resource.getFilename()).build().toString()).body(resource);
    }

    @DeleteMapping
    Map<String, Object> delete(@RequestParam String path) throws Exception {
        files.delete(path); return Map.of("deleted", path);
    }
}
