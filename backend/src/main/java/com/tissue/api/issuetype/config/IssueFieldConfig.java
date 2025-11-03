package com.tissue.api.issuetype.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issuetype.domain.policy.FieldDefintionPolicy;

@Configuration
public class IssueFieldConfig {

	@Bean
	public FieldDefintionPolicy fieldDefintionPolicy(
		@Value("${tissue.issue.field.max-enum-options:100}") int maxEnumOptions
	) {
		return new FieldDefintionPolicy(
			maxEnumOptions
		);
	}
}
