package com.tissue.api.issue.application.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record CreateWorkflowCommand(
	String workspaceCode,
	String label,
	List<StepCommand> steps,
	List<TransitionCommand> transitions
) {
	public record StepCommand(String tempKey, String label, boolean isInitial, boolean isFinal) {
	}

	public record TransitionCommand(String label, boolean isMainFlow, String sourceTempKey, String targetTempKey) {
	}
}
