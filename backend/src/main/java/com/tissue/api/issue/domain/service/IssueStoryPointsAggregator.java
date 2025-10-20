package com.tissue.api.issue.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.model.Issue;

@Component
public class IssueStoryPointsAggregator {

	public static Integer calculateTotalStoryPoints(Issue issue) {
		return issue.getChildIssues().stream()
			.filter(child -> child.getStoryPoint() != null)
			.mapToInt(Issue::getStoryPoint)
			.sum();
	}

	public static Integer calculateCompletedTotalStoryPoints(Issue issue) {
		return issue.getChildIssues().stream()
			.filter(Issue::isDone)
			.filter(child -> child.getStoryPoint() != null)
			.mapToInt(Issue::getStoryPoint)
			.sum();
	}
}
