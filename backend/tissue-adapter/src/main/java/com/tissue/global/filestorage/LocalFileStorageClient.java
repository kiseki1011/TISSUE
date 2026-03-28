package com.tissue.global.filestorage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalFileStorageClient implements FileStorageClient {

    private final Path rootPath;

    public LocalFileStorageClient(String rootPath) {
        this.rootPath = Path.of(rootPath);
    }

    @Override
    public StoredFile store(String directory, String filename, InputStream input, long size) {
        try {
            Path dirPath = rootPath.resolve(directory);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(filename);
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);

            String storedPath = directory + "/" + filename;
            log.debug("File stored locally: {}", storedPath);
            return new StoredFile(storedPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally: " + filename, e);
        }
    }

    @Override
    public Optional<FileResource> load(String storedPath) {
        try {
            Path filePath = rootPath.resolve(storedPath);
            if (!Files.exists(filePath)) {
                return Optional.empty();
            }
            long fileSize = Files.size(filePath);
            InputStream inputStream = Files.newInputStream(filePath);
            return Optional.of(new FileResource(inputStream, fileSize));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + storedPath, e);
        }
    }

    @Override
    public void delete(String storedPath) {
        try {
            Path filePath = rootPath.resolve(storedPath);
            Files.deleteIfExists(filePath);
            log.debug("File deleted locally: {}", storedPath);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", storedPath, e);
        }
    }
}
