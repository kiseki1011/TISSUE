package com.tissue.api.issue.domain.validator.checker;

import com.tissue.api.issue.domain.Issue;

public interface CircularDependencyChecker {

	void validateNoCircularDependency(Issue sourceIssue, Issue targetIssue);
}
