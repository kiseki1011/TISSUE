package com.tissue.api.issue.workflow.presentation.dto.request;

import java.util.List;

import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;

public record CreateWorkflowRequest(
	String label,
	List<StatusRequest> statusRequests,
	List<TransitionRequest> transitionRequests
) {
	public CreateWorkflowCommand toCommand(String workspaceCode) {
		List<CreateWorkflowCommand.StatusCommand> statusCommands = statusRequests.stream()
			.map(s -> new CreateWorkflowCommand.StatusCommand(s.tempKey(), s.label(), s.isInitial(), s.isFinal()))
			.toList();

		List<CreateWorkflowCommand.TransitionCommand> transitionCommands = transitionRequests.stream()
			.map(t -> new CreateWorkflowCommand.TransitionCommand(t.label(), t.isMainFlow(), t.sourceTempKey(),
				t.targetTempKey()))
			.toList();

		return CreateWorkflowCommand.builder()
			.workspaceCode(workspaceCode)
			.label(label)
			.steps(statusCommands)
			.transitions(transitionCommands)
			.build();
	}
}
