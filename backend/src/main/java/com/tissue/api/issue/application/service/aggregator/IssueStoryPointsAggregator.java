package com.tissue.api.issue.application.service.aggregator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueStoryPointsAggregator {

	private final IssueQueryRepository issueQueryRepo;

	public Integer sumChildrenStoryPoints(Issue issue) {
		return issueQueryRepo.sumChildrenStoryPoints(
			issue.getWorkspaceKey(),
			issue.getKey()
		);
	}

	public Integer sumCompletedChildrenStoryPoints(Issue issue) {
		return issueQueryRepo.sumCompletedChildrenStoryPoints(
			issue.getWorkspaceKey(),
			issue.getKey()
		);
	}
}
