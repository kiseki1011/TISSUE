package com.tissue.global.filestorage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tissue.storage")
public class FileStorageProperties {

    private String storageType = "local";
    private String storagePath = "./tissue-storage";

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
