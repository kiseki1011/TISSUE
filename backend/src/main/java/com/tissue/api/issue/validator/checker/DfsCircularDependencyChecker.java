package com.tissue.api.issue.validator.checker;

import java.util.HashSet;
import java.util.Set;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.enums.IssueRelationType;

public class DfsCircularDependencyChecker implements CircularDependencyChecker {

	@Override
	public void validateNoCircularDependency(Issue sourceIssue, Issue targetIssue) {
		Set<Issue> visited = new HashSet<>();
		visited.add(sourceIssue);
		validateCircular(targetIssue, visited);
	}

	private void validateCircular(Issue current, Set<Issue> visited) {
		for (IssueRelation relation : current.getOutgoingRelations()) {
			if (relation.getRelationType() != IssueRelationType.BLOCKS) {
				continue;
			}

			Issue nextIssue = relation.getTargetIssue();
			if (!visited.add(nextIssue)) {
				throw new InvalidOperationException("Circular dependency detected in blocking chain.");
			}

			validateCircular(nextIssue, visited);
			visited.remove(nextIssue);
		}
	}
}
