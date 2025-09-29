package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.issue.base.domain.model.vo.Label;

import lombok.Builder;

@Builder
public record CreateWorkflowCommand(
	String workspaceKey,
	Label label,
	String description,
	List<StatusCommand> statuses,
	List<TransitionCommand> transitions
) {
	public record StatusCommand(String tempKey, Label label, String description, boolean initial, boolean terminal) {
	}

	public record TransitionCommand(Label label, String description, boolean mainFlow, String sourceTempKey,
									String targetTempKey) {
	}
}
