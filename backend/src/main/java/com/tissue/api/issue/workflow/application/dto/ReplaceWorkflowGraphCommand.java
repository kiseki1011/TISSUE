package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.workflow.domain.service.EntityRef;
import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;

public record ReplaceWorkflowGraphCommand(
	String workspaceKey,
	Long workflowId,
	Long version,
	List<StatusCommand> statusCommands,
	List<TransitionCommand> transitionCommands
) {
	public record StatusCommand(
		EntityRef ref,
		String label,
		String description,
		ColorType color,
		boolean initial,
		boolean terminal
	) {
		public WorkflowGraphValidator.StatusValidationData toValidationData() {
			String key = ref.isExisting()
				? String.valueOf(ref.id())
				: ref.tempKey();
			return new WorkflowGraphValidator.StatusValidationData(key, initial, terminal);
		}
	}

	public record TransitionCommand(
		EntityRef ref,
		String label,
		String description,
		EntityRef source,
		EntityRef target
	) {
		public WorkflowGraphValidator.TransitionValidationData toValidationData() {
			String srcKey = source.isExisting()
				? String.valueOf(source.id())
				: source.tempKey();
			String trgKey = target.isExisting()
				? String.valueOf(target.id())
				: target.tempKey();
			return new WorkflowGraphValidator.TransitionValidationData(srcKey, trgKey);
		}
	}
}
