package com.tissue.api.issue.application.service.finder;

import static com.tissue.api.common.util.IssueKeyUtil.*;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.exception.IssueNotFoundException;
import com.tissue.api.issue.domain.port.out.IssueQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFinder {

	private final IssueQueryRepository issueQueryRepo;

	public Issue findBy(String issueKey, String workspaceKey) {
		return issueQueryRepo.findByKeyAndWorkspaceKey(issueKey, workspaceKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, extractProjectKey(issueKey), workspaceKey));
	}

	public List<Issue> findAllBy(Collection<String> issueKeys, String workspaceKey) {
		return issueQueryRepo.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
	}

	public Issue findIssueInSprint(String sprintKey, String issueKey, String workspaceKey) {
		return issueQueryRepo.findIssueInSprint(sprintKey, issueKey, workspaceKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, extractProjectKey(issueKey), workspaceKey));
	}
}
