package com.tissue.api.issue.application.service.reader;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;
import com.tissue.api.issue.infrastructure.repository.IssueTypeRepository;

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
