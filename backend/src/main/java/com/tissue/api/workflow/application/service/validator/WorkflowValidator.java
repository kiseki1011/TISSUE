package com.tissue.api.workflow.application.service.validator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.dto.GuardConfigData;
import com.tissue.api.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.exception.DuplicateGuardTypeException;
import com.tissue.api.workflow.domain.exception.DuplicateWorkflowException;
import com.tissue.api.workflow.domain.exception.WorkflowStateInUseException;
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
			throw new DuplicateWorkflowException(label.getNormalized(), project);
		}
	}

	public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
		List<WorkflowState> statesToCheck = statesToDelete.stream()
			.filter(state -> !state.getCategory().isDone())
			.toList();

		if (statesToCheck.isEmpty()) {
			return;
		}

		List<Long> stateIds = statesToCheck.stream()
			.map(WorkflowState::getId)
			.toList();

		List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

		if (!usedStateIds.isEmpty()) {
			String usedStateNames = statesToCheck.stream()
				.filter(s -> usedStateIds.contains(s.getId()))
				.map(WorkflowState::getDisplayLabel)
				.collect(Collectors.joining(", "));

			throw new WorkflowStateInUseException(usedStateNames);
		}
	}

	public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
		boolean dup = !usedTypes.add(g.guardType());
		if (dup) {
			throw new DuplicateGuardTypeException(g.guardType());
		}
	}
}
