package com.tissue.api.issue.service.query;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueQueryService {

	private final IssueRepository issueRepository;

	@Transactional(readOnly = true)
	public Issue findIssue(String issueKey, String code) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, code));
	}

	@Transactional(readOnly = true)
	public List<Issue> findIssues(Collection<String> issueKeys, String workspaceCode) {
		List<Issue> issues = issueRepository.findByIssueKeyInAndWorkspaceCode(issueKeys, workspaceCode);

		if (issues.size() != issueKeys.size()) {
			throw new InvalidOperationException("Some issues do not exist.");
		}

		return issues;
	}
}
