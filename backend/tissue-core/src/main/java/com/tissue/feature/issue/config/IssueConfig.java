package com.tissue.feature.issue.config;

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
        IssueProperties.Field.Decimal decimal = properties.getField().getDecimal();

        return new IssuePolicy(
                properties.getMaxReviewers(),
                decimal.getScale(),
                decimal.getRounding(),
                decimal.getMaxIntegerDigits(),
                decimal.getMaxFractionDigits(),
                properties.getField().getMaxSelectOptions());
    }
}
