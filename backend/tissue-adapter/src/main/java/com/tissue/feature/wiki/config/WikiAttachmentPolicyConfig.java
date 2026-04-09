package com.tissue.feature.wiki.config;

import com.tissue.feature.wiki.domain.policy.WikiAttachmentPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WikiAttachmentProperties.class)
public class WikiAttachmentPolicyConfig {

    @Bean
    public WikiAttachmentPolicy wikiAttachmentPolicy(WikiAttachmentProperties properties) {
        return new WikiAttachmentPolicy(properties.getMaxAttachmentsPerDocument(), properties.getAllowedContentTypes());
    }
}
