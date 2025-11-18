package com.tissue.api.issue.application.service.finder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.exception.IssueNotFoundException;

import lombok.RequiredArgsConstructor;

// TODO: project 애그리거트 추가 후 projectKey 관련 리팩토링
@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	public Issue findIssue(String issueKey, String workspaceCode) {
		return issueQueryRepo.findByKeyAndWorkspace_Key(issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceCode));
	}

	public List<Issue> findIssues(Collection<String> issueKeys, String workspaceCode) {
		return issueQueryRepo.findByKeyInAndWorkspace_Key(issueKeys, workspaceCode);
	}

	public Issue findIssueInSprint(String sprintKey, String issueKey, String workspaceCode) {
		return issueQueryRepo.findIssueInSprint(sprintKey, issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceCode));
	}
}
