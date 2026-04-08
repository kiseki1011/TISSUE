package com.tissue.feature.attachment.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tissue.attachment")
public class AttachmentProperties {

    private String storageType = "local";
    private String storagePath = "./tissue-storage";
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
            "application/gzip",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/x-hwp",
            "application/hwp+zip");

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
