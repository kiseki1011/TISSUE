package com.tissue.api.issue.config;

import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.policy.FieldValuePolicy;

import jakarta.annotation.PostConstruct;

// TODO: Consider using @ConfigurationProperties
@Configuration
public class IssueConfig {

	@Value("${tissue.issue.max-reviewers}")
	private int maxReviewers;

	@Bean
	public FieldValuePolicy fieldValuePolicy(
		@Value("${tissue.issue.field.decimal.scale:6}") int decimalScale,
		@Value("${tissue.issue.field.decimal.rounding:HALF_UP}") RoundingMode roundingMode,
		@Value("${tissue.issue.field.decimal.digits.integer:14}") int maxIntegerDigits,
		@Value("${tissue.issue.field.decimal.digits.fraction:6}") int maxFractionDigits
	) {
		return new FieldValuePolicy(
			decimalScale,
			roundingMode,
			maxIntegerDigits,
			maxFractionDigits
		);
	}
	
	// TODO: 실무에서 이런 @PostConstruct를 통해 설정을 값을 주입하는 형태의 설계는 일반적인가?
	@PostConstruct
	public void init() {
		Issue.setLimits(maxReviewers);
	}
}
