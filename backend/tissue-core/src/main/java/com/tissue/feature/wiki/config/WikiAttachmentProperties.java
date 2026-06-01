package com.tissue.feature.wiki.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tissue.wiki.attachment")
public class WikiAttachmentProperties {

    private int maxAttachmentsPerDocument = 20;

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
}
