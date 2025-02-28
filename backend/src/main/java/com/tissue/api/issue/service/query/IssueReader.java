package com.tissue.api.issue.service.query;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueReader {

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
