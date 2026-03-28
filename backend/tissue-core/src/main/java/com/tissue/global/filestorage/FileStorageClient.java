package com.tissue.global.filestorage;

import java.io.InputStream;
import java.util.Optional;

public interface FileStorageClient {

    StoredFile store(String directory, String filename, InputStream input, long size);

    Optional<FileResource> load(String storedPath);

    void delete(String storedPath);
}
