package com.tissue.api.issue.application.service.calculator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueHierarchy;
import com.tissue.api.issue.domain.enums.ProgressType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CountBasedProgressCalculator implements IssueProgressCalculator {

	private final IssueQueryRepository issueQueryRepo;

	@Override
	public Integer doCalculate(Issue issue) {
		int total = issueQueryRepo.countChildren(
			issue.getWorkspaceKey(),
			issue.getKey()
		);

		int completed = issueQueryRepo.countCompletedChildren(
			issue.getWorkspaceKey(),
			issue.getKey()
		);

		return (int)((completed * 100) / total);
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

		return issue.getHierarchy() != IssueHierarchy.MICROTASK;
	}

	@Override
	public ProgressType getType() {
		return ProgressType.COUNT_BASED;
	}
}
