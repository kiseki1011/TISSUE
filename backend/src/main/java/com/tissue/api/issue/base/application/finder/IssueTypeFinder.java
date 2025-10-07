package com.tissue.api.issue.base.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueTypeRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

	private final IssueTypeRepository issueTypeRepository;

	public IssueType findIssueType(String workspaceKey, Long id) {
		return issueTypeRepository.findByWorkspace_KeyAndId(workspaceKey, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"IssueType not found: workspaceKey=" + workspaceKey + ", key=" + id));
	}

	public IssueType findIssueType(Workspace workspace, Long id) {
		return issueTypeRepository.findByWorkspaceAndId(workspace, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"IssueType not found: workspaceKey=" + workspace.getKey() + ", key=" + id));
	}
}
