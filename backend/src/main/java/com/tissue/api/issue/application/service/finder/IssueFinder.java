package com.tissue.api.issue.application.service.finder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.issue.infrastructure.repository.IssueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueRepository issueRepository;

	public Issue findIssue(
		String issueKey,
		String workspaceCode
	) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, workspaceCode));
	}

	public List<Issue> findIssues(
		Collection<String> issueKeys,
		String workspaceCode
	) {
		List<Issue> issues = issueRepository.findByIssueKeyInAndWorkspaceCode(issueKeys, workspaceCode);

		if (issues.size() != issueKeys.size()) {
			throw new ResourceNotFoundException("Some issues do not exist.");
		}

		return issues;
	}

	public Issue findIssueInSprint(
		String sprintKey,
		String issueKey,
		String workspaceCode
	) {
		return issueRepository.findIssueInSprint(sprintKey, issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, sprintKey, workspaceCode));
	}
}
