package com.tissue.feature.wiki.config;

import com.tissue.global.file.AttachmentContentTypes;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tissue.wiki.attachment")
public class WikiAttachmentProperties {

    private int maxAttachmentsPerDocument = 20;

    private List<String> allowedContentTypes = AttachmentContentTypes.DEFAULT;
}
