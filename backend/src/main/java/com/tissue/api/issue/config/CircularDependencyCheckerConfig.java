package com.tissue.api.issue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.common.exception.type.InvalidConfigurationException;
import com.tissue.api.issue.validator.checker.CachedDfsCircularDependencyChecker;
import com.tissue.api.issue.validator.checker.CircularDependencyChecker;
import com.tissue.api.issue.validator.checker.DfsCircularDependencyChecker;

@Configuration
public class CircularDependencyCheckerConfig {

	@Bean
	public CircularDependencyChecker circularDependencyChecker(
		@Value("${circular.dependency.checker.type:dfs}") String checkerType
	) {
		return switch (checkerType) {
			case "dfs" -> new DfsCircularDependencyChecker();
			case "cacheddfs" -> new CachedDfsCircularDependencyChecker();
			default -> throw new InvalidConfigurationException("Unknown checker type: " + checkerType);
		};
	}
}
