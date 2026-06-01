package com.tissue.feature.issue.config;

import com.tissue.global.file.AttachmentContentTypes;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "tissue.issue.attachment")
public class IssueAttachmentProperties {

    private int maxAttachmentsPerIssue = 20;

    private List<String> allowedContentTypes = AttachmentContentTypes.DEFAULT;
}
