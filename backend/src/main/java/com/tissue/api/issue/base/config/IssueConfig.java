package com.tissue.api.issue.base.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.base.domain.policy.IssueFieldPolicy;

@Configuration
public class IssueConfig {

	@Bean
	public IssueFieldPolicy issueFieldPolicy(
		@Value("${tissue.issue.max-enum-options:100}") int maxEnumOptions
	) {
		return new IssueFieldPolicy(maxEnumOptions);
	}
}
