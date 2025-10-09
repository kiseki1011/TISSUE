package com.tissue.api.issue.config;

import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.domain.policy.FieldValuePolicy;

@Configuration
public class IssueConfig {

	// TODO: Consider using @ConfigurationProperties
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
}
