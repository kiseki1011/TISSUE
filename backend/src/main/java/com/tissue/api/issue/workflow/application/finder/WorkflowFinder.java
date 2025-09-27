package com.tissue.api.issue.workflow.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.workflow.domain.model.Workflow;
import com.tissue.api.issue.workflow.infrastructure.repository.WorkflowRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowFinder {

	private final WorkflowRepository workflowRepository;

	public Workflow findWorkflow(Workspace workspace, Long id) {
		return workflowRepository.findByWorkspaceAndId(workspace, id)
			.orElseThrow(() -> new ResourceNotFoundException(
				"Workflow not found: workspaceKey=" + workspace.getKey() + ", id=" + id));
	}
}
