package com.tissue.issuetype.adapter.in.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.issuetype.domain.policy.FieldDefintionPolicy;

@Configuration
public class IssueFieldConfig {

	@Bean
	public FieldDefintionPolicy fieldDefintionPolicy(
		@Value("${tissue.issue.policy.field.max-enum-options:100}") int maxEnumOptions
	) {
		return new FieldDefintionPolicy(
			maxEnumOptions
		);
	}
}
