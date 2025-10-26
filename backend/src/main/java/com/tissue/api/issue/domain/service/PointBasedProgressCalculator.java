package com.tissue.api.issue.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.Issue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PointBasedProgressCalculator implements IssueProgressCalculator {

	private final IssueStoryPointsAggregator aggregator;

	@Override
	public Integer doCalculate(Issue issue) {
		int totalPoints = aggregator.calculateTotalStoryPoints(issue);
		int completedPoints = aggregator.calculateCompletedTotalStoryPoints(issue);

		return (int)((completedPoints * 100) / totalPoints);
	}

	@Override
	public boolean supports(Issue issue) {
		if (issue.getChildIssues().isEmpty()) {
			return false;
		}

		return issue.getHierarchy() == IssueHierarchy.EPIC;
	}

	@Override
	public ProgressType getType() {
		return ProgressType.POINT_BASED;
	}
}
