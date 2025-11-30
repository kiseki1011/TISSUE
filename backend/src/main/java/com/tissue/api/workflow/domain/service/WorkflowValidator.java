package com.tissue.api.workflow.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	private final WorkflowRepository workflowRepo;

	public void ensureLabelUnique(Project project, Label label) {
		boolean dup = workflowRepo.existsByProjectAndLabel_Normalized(project, label.getNormalized());
		if (dup) {
			// TODO: DuplicateWorkflowException vs DuplicateWorkflowLabelException
			throw new RuntimeException("Label cannot be duplicate for workflow in a workspace scope.");
		}
	}
}
