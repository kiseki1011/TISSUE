package com.tissue.api.issue.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.model.Issue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PointBasedProgressCalculator implements IssueProgressCalculator {

	private final IssueStoryPointsAggregator storyPointsAggregator;

	@Override
	public Integer calculate(Issue issue) {
		int totalPoints = storyPointsAggregator.calculateTotalStoryPoints(issue);

		if (totalPoints == 0) {
			return null;
		}

		int completedPoints = storyPointsAggregator.calculateCompletedTotalStoryPoints(issue);

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
