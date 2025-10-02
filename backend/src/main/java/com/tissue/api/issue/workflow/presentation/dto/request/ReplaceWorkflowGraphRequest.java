package com.tissue.api.issue.workflow.presentation.dto.request;

import java.util.List;

import com.tissue.api.issue.workflow.application.dto.ReplaceWorkflowGraphCommand;

public record ReplaceWorkflowGraphRequest(
	Long version,
	List<StatusDto> statuses,
	List<TransitionDto> transitions
) {
	public record StatusDto(Long id, String tempKey, String label, String description, boolean initial,
							boolean terminal) {
	}

	public record TransitionDto(Long id, String tempKey, String label, String description, String sourceKey,
								String targetKey, boolean mainFlow) {
	}

	public ReplaceWorkflowGraphCommand toCommand(String workspaceKey, Long workflowId) {
		return new ReplaceWorkflowGraphCommand(
			workspaceKey,
			workflowId,
			version,
			statuses.stream()
				.map(s -> new ReplaceWorkflowGraphCommand.StatusCmd(
					s.id(), s.tempKey(), s.label(), s.description(), s.initial(), s.terminal())
				)
				.toList(),
			transitions.stream()
				.map(t -> new ReplaceWorkflowGraphCommand.TransitionCmd(
					t.id(), t.tempKey(), t.label(), t.description(), t.sourceKey(), t.targetKey(), t.mainFlow())
				)
				.toList()
		);
	}
}
