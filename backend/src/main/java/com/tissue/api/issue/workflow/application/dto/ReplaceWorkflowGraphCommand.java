package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;

public record ReplaceWorkflowGraphCommand(
	String workspaceKey,
	Long workflowId,
	Long version,
	List<ReplaceStatusCommand> replaceStatusCommands,
	List<ReplaceTransitionCommand> replaceTransitionCommands
) {
	public record ReplaceStatusCommand(
		WorkflowGraphValidator.EntityRef ref,
		String label,
		String description,
		boolean initial,
		boolean terminal
	) {
		public WorkflowGraphValidator.StatusInfo toInfo() {
			String key = ref.isExisting()
				? String.valueOf(ref.id())
				: ref.tempKey();
			return new WorkflowGraphValidator.StatusInfo(key, initial, terminal);
		}
	}

	public record ReplaceTransitionCommand(
		WorkflowGraphValidator.EntityRef ref,
		String label,
		String description,
		WorkflowGraphValidator.EntityRef source,
		WorkflowGraphValidator.EntityRef target,
		boolean mainFlow
	) {
		public WorkflowGraphValidator.TransitionInfo toInfo() {
			String srcKey = source.isExisting()
				? String.valueOf(source.id())
				: source.tempKey();
			String trgKey = target.isExisting()
				? String.valueOf(target.id())
				: target.tempKey();
			return new WorkflowGraphValidator.TransitionInfo(srcKey, trgKey);
		}
	}
}
