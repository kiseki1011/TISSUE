package com.tissue.api.issue.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@ConfigurationProperties(prefix = "api.issue")
public record IssueProperties(
	CircularDependencyCacheProperties circularDependencyCache
) {
	public record CircularDependencyCacheProperties(
		@Min(100) @Max(10000)
		int size,

		@Min(1)
		int duration
	) {
	}
}
