package com.tissue.api.issue.base.domain.service;

import com.tissue.api.issue.base.domain.model.Issue;

public interface CircularDependencyValidator {
	/**
	 * Validates that there are no circular dependencies between issues.
	 * Throws an InvalidOperationException if a circular dependency is detected.
	 */
	void validateNoCircularDependency(Issue sourceIssue, Issue targetIssue);
}
