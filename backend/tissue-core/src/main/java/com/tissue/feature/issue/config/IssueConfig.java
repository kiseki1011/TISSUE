package com.tissue.feature.issue.config;

import com.tissue.feature.issue.domain.policy.FieldValuePolicy;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class IssueConfig {

    private final IssueProperties properties;

    @Bean
    public IssuePolicy issuePolicy() {
        return new IssuePolicy(properties.getMaxReviewers());
    }

    @Bean
    public FieldValuePolicy fieldValuePolicy() {
        IssueProperties.Field.Decimal decimal = properties.getField().getDecimal();
        return new FieldValuePolicy(
                decimal.getScale(),
                decimal.getRounding(),
                decimal.getMaxIntegerDigits(),
                decimal.getMaxFractionDigits());
    }
}
