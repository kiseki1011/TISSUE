package com.tissue.api.workflow.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.repository.WorkflowRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	private final WorkflowRepository workflowRepository;
	private final IssueQueryRepository issueRepository;

	public void ensureLabelUnique(Project project, Label label) {
		boolean dup = workflowRepository.existsByProjectAndLabel_Normalized(project, label.getNormalized());
		if (dup) {
			// TODO: DuplicateWorkflowException vs DuplicateWorkflowLabelException
			throw new RuntimeException("Label cannot be duplicate for workflow in a workspace scope.");
		}
	}

	public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
		if (statesToDelete.isEmpty()) {
			return;
		}

		List<Long> stateIds = statesToDelete.stream()
			.map(WorkflowState::getId)
			.toList();

		// DB 조회
		boolean inUse = issueRepository.existsByCurrentStateIdIn(stateIds);

		if (inUse) {
			// TODO: WorkflowStateInUseException
			throw new RuntimeException(
				"Cannot delete workflow states that are currently assigned to active issues."
			);
		}
	}
}
