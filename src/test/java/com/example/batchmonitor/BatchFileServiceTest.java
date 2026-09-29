package com.example.batchmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class BatchFileServiceTest {
    @Test
    void filesStayInsideConfiguredRoot() throws Exception {
        var root = Files.createTempDirectory("batch-monitor-files");
        var service = new BatchFileService(root.toString());
        assertEquals("incoming/input.csv", service.upload("incoming",
                new MockMultipartFile("file", "input.csv", "text/csv", "id\n1".getBytes())));
        assertEquals(1, service.list("incoming").size());
        assertThrows(IllegalArgumentException.class, () -> service.list("../outside"));
        assertThrows(IllegalArgumentException.class, () -> service.download("../outside.txt"));
    }
}
