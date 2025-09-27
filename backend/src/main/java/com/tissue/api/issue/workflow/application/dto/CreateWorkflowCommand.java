package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record CreateWorkflowCommand(
	String workspaceCode,
	Label label,
	String description,
	List<StatusCommand> statuses,
	List<TransitionCommand> transitions
) {
	public record StatusCommand(String tempKey, Label label, String description, boolean isInitial, boolean isFinal) {
	}

	public record TransitionCommand(Label label, String description, boolean isMainFlow, String sourceTempKey,
									String targetTempKey) {
	}
}
