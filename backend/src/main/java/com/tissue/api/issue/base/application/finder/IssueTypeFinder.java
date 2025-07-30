package com.tissue.api.issue.base.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.base.domain.model.IssueTypeDefinition;
import com.tissue.api.issue.base.infrastructure.repository.IssueTypeRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

	private final IssueTypeRepository issueTypeRepository;

	public IssueTypeDefinition findIssueType(
		String workspaceCode,
		String key
	) {
		// TODO: Consider making a custom exception IssueTypeNotFoundException
		return issueTypeRepository.findByWorkspaceCodeAndKey(workspaceCode, key)
			.orElseThrow(() -> new ResourceNotFoundException(
				"IssueTypeDefinition not found: workspaceCode=" + workspaceCode + ", key=" + key));
	}
}
