package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.domain.service.EntityRef;
import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;

import lombok.Builder;

@Builder
public record CreateWorkflowCommand(
	String workspaceKey,
	Label label,
	String description,
	ColorType color,
	List<StatusCommand> statusCommands,
	List<TransitionCommand> transitionCommands
) {
	public record StatusCommand(
		EntityRef ref,
		Label label,
		String description,
		ColorType color,
		boolean initial,
		boolean terminal
	) {
		public WorkflowGraphValidator.StatusValidationData toValidationData() {
			return new WorkflowGraphValidator.StatusValidationData(
				ref.tempKey(),
				initial,
				terminal
			);
		}
	}

	public record TransitionCommand(
		Label label,
		String description,
		EntityRef sourceRef,
		EntityRef targetRef
	) {
		public WorkflowGraphValidator.TransitionValidationData toValidationData() {
			return new WorkflowGraphValidator.TransitionValidationData(
				sourceRef.tempKey(),
				targetRef.tempKey()
			);
		}
	}
}
