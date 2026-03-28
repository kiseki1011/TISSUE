package com.tissue.global.filestorage;

import com.tissue.feature.attachment.config.AttachmentProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@RequiredArgsConstructor
public class FileStorageConfig {

    private final AttachmentProperties properties;

    @Bean
    public FileStorageClient fileStorageClient() {
        if ("s3".equalsIgnoreCase(properties.getStorageType())) {
            return createS3Client();
        }
        return new LocalFileStorageClient(properties.getStoragePath());
    }

    private S3FileStorageClient createS3Client() {
        AttachmentProperties.S3 s3Props = properties.getS3();

        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Props.getAccessKey(), s3Props.getSecretKey())))
                .region(Region.of(s3Props.getRegion()));

        if (!s3Props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3Props.getEndpoint())).forcePathStyle(true);
        }

        return new S3FileStorageClient(builder.build(), s3Props.getBucket());
    }
}
