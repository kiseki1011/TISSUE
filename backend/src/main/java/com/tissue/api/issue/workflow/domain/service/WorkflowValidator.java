package com.tissue.api.issue.workflow.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.DuplicateResourceException;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.infrastructure.repository.WorkflowRepository;
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
}
