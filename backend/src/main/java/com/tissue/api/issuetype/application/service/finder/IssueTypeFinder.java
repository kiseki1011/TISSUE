package com.tissue.api.issuetype.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.exception.IssueTypeNotFoundException;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

	private final IssueTypeQueryRepository issueTypeQueryRepository;

	public IssueType findByIdAndWorkspaceKey(Long issueTypeId, String workspaceKey) {
		return issueTypeQueryRepository.findByIdAndWorkspace_Key(issueTypeId, workspaceKey)
			.orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId));
	}

	public IssueType findByIdAndWorkspace(Long issueTypeId, Workspace workspace) {
		return issueTypeQueryRepository.findByIdAndWorkspace(issueTypeId, workspace)
			.orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId));
	}
}
