package com.tissue.feature.attachment.config;

import com.tissue.feature.attachment.domain.policy.IssueAttachmentPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AttachmentProperties.class)
public class AttachmentPolicyConfig {

    @Bean
    public IssueAttachmentPolicy issueAttachmentPolicy(AttachmentProperties properties) {
        return new IssueAttachmentPolicy(properties.getMaxAttachmentsPerIssue(), properties.getAllowedContentTypes());
    }
}
