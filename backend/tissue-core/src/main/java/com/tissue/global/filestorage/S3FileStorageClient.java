package com.tissue.global.filestorage;

import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@RequiredArgsConstructor
public class S3FileStorageClient implements FileStorageClient {

    private final S3Client s3Client;
    private final String bucket;

    @Override
    public StoredFile store(String directory, String filename, InputStream input, long size) {
        String key = directory + "/" + filename;

        PutObjectRequest request =
                PutObjectRequest.builder().bucket(bucket).key(key).build();

        s3Client.putObject(request, RequestBody.fromInputStream(input, size));
        log.debug("File stored in S3: {}", key);
        return new StoredFile(key);
    }

    @Override
    public Optional<FileResource> load(String storedPath) {
        try {
            HeadObjectRequest headRequest =
                    HeadObjectRequest.builder().bucket(bucket).key(storedPath).build();
            long fileSize = s3Client.headObject(headRequest).contentLength();

            GetObjectRequest getRequest =
                    GetObjectRequest.builder().bucket(bucket).key(storedPath).build();
            InputStream inputStream = s3Client.getObject(getRequest);

            return Optional.of(new FileResource(inputStream, fileSize));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String storedPath) {
        try {
            DeleteObjectRequest request =
                    DeleteObjectRequest.builder().bucket(bucket).key(storedPath).build();
            s3Client.deleteObject(request);
            log.debug("File deleted from S3: {}", storedPath);
        } catch (Exception e) {
            log.warn("Failed to delete file from S3: {}", storedPath, e);
        }
    }
}
