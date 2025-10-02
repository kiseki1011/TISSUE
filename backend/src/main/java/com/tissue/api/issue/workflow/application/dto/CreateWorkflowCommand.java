package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;

import lombok.Builder;

@Builder
public record CreateWorkflowCommand(
	String workspaceKey,
	Label label,
	String description,
	List<CreateStatusCommand> createStatusCommands,
	List<CreateTransitionCommand> createTransitionCommands
) {
	public record CreateStatusCommand(
		WorkflowGraphValidator.EntityRef ref,
		Label label,
		String description,
		boolean initial,
		boolean terminal
	) {
		public WorkflowGraphValidator.StatusInfo toInfo() {
			return new WorkflowGraphValidator.StatusInfo(
				ref.tempKey(),
				initial,
				terminal
			);
		}
	}

	public record CreateTransitionCommand(
		Label label,
		String description,
		boolean mainFlow,
		WorkflowGraphValidator.EntityRef sourceRef,
		WorkflowGraphValidator.EntityRef targetRef
	) {
		public WorkflowGraphValidator.TransitionInfo toInfo() {
			return new WorkflowGraphValidator.TransitionInfo(
				sourceRef.tempKey(),
				targetRef.tempKey()
			);
		}
	}
}
