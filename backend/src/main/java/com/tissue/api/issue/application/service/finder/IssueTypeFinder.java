package com.tissue.api.issue.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

	private final IssueTypeQueryRepository issueTypeQueryRepository;

	public IssueType findIssueType(String workspaceKey, Long id) {
		return issueTypeQueryRepository.findByWorkspace_KeyAndId(workspaceKey, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"IssueType not found: workspaceKey=" + workspaceKey + ", key=" + id));
	}

	public IssueType findIssueType(Workspace workspace, Long id) {
		return issueTypeQueryRepository.findByWorkspaceAndId(workspace, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"IssueType not found: workspaceKey=" + workspace.getKey() + ", key=" + id));
	}
}
