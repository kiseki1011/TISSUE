package com.tissue.feature.issue.config;

import com.tissue.feature.issue.domain.policy.IssueAttachmentPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IssueAttachmentProperties.class)
public class IssueAttachmentPolicyConfig {

    @Bean
    public IssueAttachmentPolicy issueAttachmentPolicy(IssueAttachmentProperties properties) {
        return new IssueAttachmentPolicy(properties.getMaxAttachmentsPerIssue(), properties.getAllowedContentTypes());
    }
}
