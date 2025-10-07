package com.tissue.api.issue.base.config;

import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.base.domain.policy.IssueFieldPolicy;

@Configuration
public class IssueConfig {

	// TODO: Consider using @ConfigurationProperties
	@Bean
	public IssueFieldPolicy issueFieldPolicy(
		@Value("${tissue.issue.field.max-enum-options:100}") int maxEnumOptions,
		@Value("${tissue.issue.field.decimal.scale:6}") int decimalScale,
		@Value("${tissue.issue.field.decimal.rounding:HALF_UP}") RoundingMode roundingMode,
		@Value("${tissue.issue.field.decimal.digits.integer:14}") int maxIntegerDigits,
		@Value("${tissue.issue.field.decimal.digits.fraction:6}") int maxFractionDigits
	) {
		return new IssueFieldPolicy(
			maxEnumOptions,
			decimalScale,
			roundingMode,
			maxIntegerDigits,
			maxFractionDigits
		);
	}
}
