package com.tissue.feature.issuetype.config;

import com.tissue.feature.issuetype.domain.policy.FieldDefintionPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IssueFieldConfig {

    @Bean
    public FieldDefintionPolicy fieldDefintionPolicy(
            @Value("${tissue.issue.policy.field.max-enum-options:100}") int maxEnumOptions) {
        return new FieldDefintionPolicy(maxEnumOptions);
    }
}
