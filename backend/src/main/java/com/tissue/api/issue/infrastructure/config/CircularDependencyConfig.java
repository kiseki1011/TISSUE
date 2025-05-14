package com.tissue.api.issue.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.domain.service.validator.CircularDependencyValidator;
import com.tissue.api.issue.domain.service.validator.DfsCircularDependencyValidator;
import com.tissue.api.issue.domain.service.validator.cache.IssueRelationDependencyCache;
import com.tissue.api.issue.infrastructure.cache.CaffeineDependencyCache;

@Configuration
public class CircularDependencyConfig {

	@Bean
	public IssueRelationDependencyCache dependencyCache(
		@Value("${api.issue.circular-dependency-cache.size:1000}") int cacheSize,
		@Value("${api.issue.circular-dependency-cache.duration:24}") int expirationHours) {
		return new CaffeineDependencyCache(cacheSize, expirationHours);
	}

	@Bean
	public CircularDependencyValidator circularDependencyChecker(IssueRelationDependencyCache dependencyCache) {
		return new DfsCircularDependencyValidator(dependencyCache);
	}
}
