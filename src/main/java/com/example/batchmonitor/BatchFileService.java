package com.example.batchmonitor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

@Service
public class BatchFileService {
    private final Path root;

    public BatchFileService(@Value("${app.files.upload-dir:${java.io.tmpdir}/spring-batch-monitor}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public List<String> list(String relativePath) throws IOException {
        Path directory = resolveDirectory(relativePath);
        if (!Files.exists(directory)) return List.of();
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).map(path -> root.relativize(path).toString().replace('\\', '/')).sorted().toList();
        }
    }

    public String upload(String relativePath, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("A non-empty file is required");
        String filename = Path.of(file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename()).getFileName().toString();
        if (filename.isBlank() || filename.equals(".") || filename.equals("..")) throw new IllegalArgumentException("Invalid file name");
        Path directory = resolveDirectory(relativePath);
        Files.createDirectories(directory);
        Path target = directory.resolve(filename).normalize();
        ensureInsideRoot(target);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return root.relativize(target).toString().replace('\\', '/');
    }

    public Resource download(String relativePath) {
        Path file = resolveFile(relativePath);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists() || !resource.isReadable()) throw new IllegalArgumentException("File not found");
        return resource;
    }

    public void delete(String relativePath) throws IOException {
        Path file = resolveFile(relativePath);
        if (!Files.deleteIfExists(file)) throw new IllegalArgumentException("File not found");
    }

    private Path resolveDirectory(String path) { return resolve(path == null || path.isBlank() ? "." : path); }
    private Path resolveFile(String path) {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("File path is required");
        Path file = resolve(path);
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("File not found");
        return file;
    }
    private Path resolve(String path) { Path resolved = root.resolve(path).normalize(); ensureInsideRoot(resolved); return resolved; }
    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(root)) throw new IllegalArgumentException("Path is outside the configured upload directory");
    }
}
