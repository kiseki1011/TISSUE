package com.tissue.api.issue.workflow.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.workflow.domain.model.WorkflowDefinition;
import com.tissue.api.issue.workflow.infrastructure.repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowRepository workflowRepository;

	public WorkflowDefinition findWorkflow(
		String workspaceCode,
		String key
	) {
		// TODO: Consider making a custom exception WorkflowNotFoundException
		return workflowRepository.findByWorkspaceCodeAndKey(workspaceCode, key)
			.orElseThrow(() -> new ResourceNotFoundException(
				"Workflow not found: workspaceCode=" + workspaceCode + ", key=" + key));
	}
}
