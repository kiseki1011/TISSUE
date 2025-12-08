package com.tissue.api.workflow.application.service.validator;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.dto.GuardConfigData;
import com.tissue.api.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.guard.GuardType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	private final WorkflowQueryRepository workflowQueryRepository;
	private final IssueQueryRepository issueRepository;

	public void ensureLabelUnique(Project project, Label label) {
		boolean dup = workflowQueryRepository.existsByProjectAndLabel_Normalized(project, label.getNormalized());
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

	// TODO: TransitionGuardRegistry로 옮겨야 할까?
	public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
		boolean dup = !usedTypes.add(g.guardType());
		if (dup) {
			// TODO: DuplicateGuardTypeException
			throw new RuntimeException("Duplicate guard type: " + g.guardType());
		}
	}
}
