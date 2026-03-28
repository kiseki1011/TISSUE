package com.tissue.feature.attachment.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tissue.attachment")
public class AttachmentProperties {

    private String storageType = "local";
    private String storagePath = "./tissue-storage";
    private long maxFileSize = 20 * 1024 * 1024; // 20MB
    private int maxAttachmentsPerIssue = 20;

    private List<String> allowedContentTypes = List.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "application/pdf",
            "text/plain",
            "text/csv",
            "application/json",
            "application/xml",
            "application/zip",
            "application/gzip");

    private S3 s3 = new S3();

    @Data
    public static class S3 {
        private String endpoint = "";
        private String region = "";
        private String bucket = "";
        private String accessKey = "";
        private String secretKey = "";
    }
}
