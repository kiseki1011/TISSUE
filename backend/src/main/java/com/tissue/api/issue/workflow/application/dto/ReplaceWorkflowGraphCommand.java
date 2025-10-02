package com.tissue.api.issue.workflow.application.dto;

import java.util.List;

public record ReplaceWorkflowGraphCommand(
	String workspaceKey,
	Long workflowId,
	Long version,
	List<StatusCmd> statuses,
	List<TransitionCmd> transitions
) {
	public record StatusCmd(Long id, String tempKey, String label, String description, boolean initial,
							boolean terminal) {
	}

	public record TransitionCmd(Long id, String tempKey, String label, String description, String sourceKey,
								String targetKey, boolean mainFlow) {
	}
}
