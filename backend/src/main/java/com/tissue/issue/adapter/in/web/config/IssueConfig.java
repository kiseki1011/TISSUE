package com.tissue.issue.adapter.in.web.config;

import com.tissue.issue.domain.policy.FieldValuePolicy;
import com.tissue.issue.domain.policy.IssuePolicy;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// TODO: @ConfigurationProperties 고려
@Configuration
public class IssueConfig {

    @Bean
    public IssuePolicy issuePolicy(
            @Value("${tissue.issue.policy.max-reviewers}") int maxReviewers) {
        return new IssuePolicy(maxReviewers);
    }

    @Bean
    public FieldValuePolicy fieldValuePolicy(
            @Value("${tissue.issue.policy.field.decimal.scale:6}") int decimalScale,
            @Value("${tissue.issue.policy.field.decimal.rounding:HALF_UP}")
                    RoundingMode roundingMode,
            @Value("${tissue.issue.policy.field.decimal.digits.integer:14}") int maxIntegerDigits,
            @Value("${tissue.issue.policy.field.decimal.digits.fraction:6}")
                    int maxFractionDigits) {
        return new FieldValuePolicy(
                decimalScale, roundingMode, maxIntegerDigits, maxFractionDigits);
    }
}
