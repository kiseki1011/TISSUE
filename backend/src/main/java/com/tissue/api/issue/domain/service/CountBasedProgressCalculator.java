package com.tissue.api.issue.domain.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.model.Issue;

@Component
public class CountBasedProgressCalculator implements IssueProgressCalculator {

	@Override
	public Integer doCalculate(Issue issue) {
		List<Issue> children = issue.getChildIssues();

		long total = children.size();

		long completed = children.stream()
			.filter(Issue::isDone)
			.count();

		return (int)((completed * 100) / total);
	}

	@Override
	public boolean supports(Issue issue) {
		if (issue.getChildIssues().isEmpty()) {
			return false;
		}

		return issue.getHierarchy() != IssueHierarchy.MICROTASK;
	}

	@Override
	public ProgressType getType() {
		return ProgressType.COUNT_BASED;
	}
}
