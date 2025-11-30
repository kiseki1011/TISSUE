package com.tissue.api.issue.domain.service.calculator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.ProgressType;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.service.aggregator.IssueStoryPointsAggregator;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PointBasedProgressCalculator implements IssueProgressCalculator {

	private final IssueStoryPointsAggregator aggregator;
	private final IssueQueryRepository issueQueryRepo;

	@Override
	public Integer doCalculate(Issue issue) {
		int totalPoints = aggregator.sumChildrenStoryPoints(issue);

		if (totalPoints == 0) {
			return 0;
		}

		int completedPoints = aggregator.sumCompletedChildrenStoryPoints(issue);

		return (int)((completedPoints * 100) / totalPoints);
	}

	@Override
	public boolean supports(Issue issue) {
		boolean noChildren = !issueQueryRepo.hasChildren(
			issue.getWorkspaceKey(),
			issue.getKey()
		);

		if (noChildren) {
			return false;
		}

		return issue.getHierarchy() == IssueHierarchy.EPIC;
	}

	@Override
	public ProgressType getType() {
		return ProgressType.POINT_BASED;
	}
}
