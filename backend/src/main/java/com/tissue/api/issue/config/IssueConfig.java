package com.tissue.api.issue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tissue.api.common.exception.type.InvalidConfigurationException;
import com.tissue.api.issue.config.properties.IssueProperties;
import com.tissue.api.issue.validator.checker.CachedDfsCircularDependencyChecker;
import com.tissue.api.issue.validator.checker.CircularDependencyChecker;
import com.tissue.api.issue.validator.checker.DfsCircularDependencyChecker;

@Configuration
public class IssueConfig {

	@Bean
	public CircularDependencyChecker circularDependencyChecker(IssueProperties properties) {
		return switch (properties.circularDependencyChecker().type()) {
			case DFS -> new DfsCircularDependencyChecker();
			case CACHED_DFS -> new CachedDfsCircularDependencyChecker(
				properties.circularDependencyCache().size(),
				properties.circularDependencyCache().duration()
			);
			default -> throw new InvalidConfigurationException("Unknown checker type");
		};
	}
}
