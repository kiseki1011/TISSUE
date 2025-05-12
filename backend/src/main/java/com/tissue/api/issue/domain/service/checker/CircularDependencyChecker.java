package com.tissue.api.issue.domain.service.checker;

import com.tissue.api.issue.domain.model.Issue;

public interface CircularDependencyChecker {

	void validateNoCircularDependency(Issue sourceIssue, Issue targetIssue);
}
