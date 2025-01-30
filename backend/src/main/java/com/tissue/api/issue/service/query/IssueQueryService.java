package com.tissue.api.issue.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
