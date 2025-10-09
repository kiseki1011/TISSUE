package com.tissue.api.workflow.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.repository.WorkflowRepository;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	private final WorkflowRepository workflowRepo;

	public void ensureLabelUnique(Workspace workspace, Label label) {
		boolean dup = workflowRepo.existsByWorkspaceAndLabel_Normalized(workspace, label.getNormalized());
		if (dup) {
			throw new DuplicateResourceException("Label cannot be duplicate for workflow in a workspace scope.");
		}
	}

	public void ensureNotSystemProvided(Workflow workflow) {
		if (workflow.isSystemProvided()) {
			throw new RuntimeException("Cannot modify system provided workflow.");
		}
	}
}
