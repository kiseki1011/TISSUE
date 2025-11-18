package com.tissue.api.workflow.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.repository.WorkflowRepository;
import com.tissue.api.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	private final WorkflowRepository workflowRepo;

	public void ensureLabelUnique(Workspace workspace, Label label) {
		boolean dup = workflowRepo.existsByWorkspaceAndLabel_Normalized(workspace, label.getNormalized());
		if (dup) {
			// TODO: DuplicateWorkflowException vs DuplicateWorkflowLabelException
			throw new RuntimeException("Label cannot be duplicate for workflow in a workspace scope.");
		}
	}

	public void ensureNotSystemProvided(Workflow workflow) {
		if (workflow.isSystemProvided()) {
			throw new RuntimeException("Cannot modify system provided workflow.");
		}
	}
}
