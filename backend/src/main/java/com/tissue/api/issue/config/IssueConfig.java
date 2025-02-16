package com.tissue.api.issue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.issue.config.properties.IssueProperties;
import com.tissue.api.issue.validator.checker.CachedDfsCircularDependencyChecker;
import com.tissue.api.issue.validator.checker.CircularDependencyChecker;

@Configuration
public class IssueConfig {

	/**
	 * Creates a component that checks circular dependencies between issues.
	 * Uses a cached DFS implementation for better performance in repeated checks.
	 *
	 * @param properties issue properties containing circular dependency cache configurations
	 * @return circular dependency checker implementation
	 */
	@Bean
	public CircularDependencyChecker circularDependencyChecker(IssueProperties properties) {
		return new CachedDfsCircularDependencyChecker(
			properties.circularDependencyCache().size(),
			properties.circularDependencyCache().duration()
		);
	}
}
