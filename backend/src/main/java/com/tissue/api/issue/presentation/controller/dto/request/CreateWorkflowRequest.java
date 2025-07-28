package com.tissue.api.issue.presentation.controller.dto.request;

import java.util.List;

import com.tissue.api.issue.application.dto.CreateWorkflowCommand;

public record CreateWorkflowRequest(
	String label,
	List<StepRequest> steps,
	List<TransitionRequest> transitions
) {
	public CreateWorkflowCommand toCommand(String workspaceCode) {
		List<CreateWorkflowCommand.StepCommand> stepCommands = steps.stream()
			.map(s -> new CreateWorkflowCommand.StepCommand(s.tempKey(), s.label(), s.isInitial(), s.isFinal()))
			.toList();

		List<CreateWorkflowCommand.TransitionCommand> transitionCommands = transitions.stream()
			.map(t -> new CreateWorkflowCommand.TransitionCommand(t.label(), t.isMainFlow(), t.sourceTempKey(),
				t.targetTempKey()))
			.toList();

		return CreateWorkflowCommand.builder()
			.workspaceCode(workspaceCode)
			.label(label)
			.steps(stepCommands)
			.transitions(transitionCommands)
			.build();
	}
}
