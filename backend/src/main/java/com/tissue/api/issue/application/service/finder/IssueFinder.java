package com.tissue.api.issue.application.service.finder;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.exception.IssueNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	public Issue findIssue(String issueKey, String workspaceCode) {
		return issueQueryRepo.findByKeyAndWorkspace_Key(issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, workspaceCode));
	}

	public List<Issue> findIssues(Collection<String> issueKeys, String workspaceCode) {
		List<Issue> issues = issueQueryRepo.findByKeyInAndWorkspace_Key(issueKeys, workspaceCode);

		// TODO: 굳이 필요한가? 몇몇 없는 이슈는 무시할까? 아니면 없던 이슈들의 키를 모아서 사용자에게 알려야할까?
		if (issues.size() != issueKeys.size()) {
			throw new ResourceNotFoundException("Some issues do not exist.");
		}

		return issues;
	}

	public Issue findIssueInSprint(String sprintKey, String issueKey, String workspaceCode) {
		return issueQueryRepo.findIssueInSprint(sprintKey, issueKey, workspaceCode)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, sprintKey, workspaceCode));
	}
}
