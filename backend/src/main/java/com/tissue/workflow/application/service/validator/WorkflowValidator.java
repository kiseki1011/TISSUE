package com.tissue.workflow.application.service.validator;

import static com.tissue.workflow.domain.enums.StateCategory.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Label;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.dto.GuardConfigData;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.exception.WorkflowExceptions;
import com.tissue.workflow.domain.guard.GuardType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

	private final WorkflowQueryRepository workflowQueryRepository;
	private final IssueQueryRepository issueRepository;

	public void ensureLabelUnique(Project project, Label label) {
		boolean dup = workflowQueryRepository.existsByProjectAndLabel_Normalized(project, label.getNormalized());
		if (dup) {
			throw WorkflowExceptions.duplicateWorkflowName(
				label.getNormalized(),
				project.getKey(),
				project.getWorkspaceKey()
			);
		}
	}

	public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
		List<WorkflowState> statesToCheck = statesToDelete.stream()
			.filter(state -> !state.isCategorizedAs(COMPLETED))
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

			throw WorkflowExceptions.workflowStateInUse(usedStateNames);
		}
	}

	public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
		boolean dup = !usedTypes.add(g.guardType());
		if (dup) {
			throw WorkflowExceptions.duplicateGuardType(g.guardType());
		}
	}
}
