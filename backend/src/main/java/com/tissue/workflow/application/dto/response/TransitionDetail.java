package com.tissue.workflow.application.dto.response;

import java.util.List;

import com.tissue.workflow.domain.WorkflowTransition;

public record TransitionDetail(
	Long id,
	String label,
	String description,
	Long sourceStateId,
	Long targetStateId,
	List<GuardDetail> guards
) {
	public static TransitionDetail from(WorkflowTransition t) {
		return new TransitionDetail(
			t.getId(),
			t.getDisplayName(),
			t.getDescription(),
			t.getSourceState().getId(),
			t.getTargetState().getId(),
			t.getGuardConfigs().stream()
				.map(GuardDetail::from)
				.toList()
		);
	}
}
