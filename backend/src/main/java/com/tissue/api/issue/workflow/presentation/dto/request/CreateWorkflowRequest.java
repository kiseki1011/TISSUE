package com.tissue.api.issue.workflow.presentation.dto.request;

import java.util.List;

import com.tissue.api.common.validator.annotation.size.LabelSize;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(
	@NotBlank @LabelSize String label,
	@Size(max = 255) String description,
	List<StatusRequest> statusRequests,
	List<TransitionRequest> transitionRequests
) {
	public CreateWorkflowCommand toCommand(String workspaceKey) {
		List<CreateWorkflowCommand.StatusCommand> statusCommands = statusRequests.stream()
			.map(s -> new CreateWorkflowCommand.StatusCommand(s.tempKey(), Label.of(s.label()), s.description(),
				s.initial(), s.terminal()))
			.toList();

		List<CreateWorkflowCommand.TransitionCommand> transitionCommands = transitionRequests.stream()
			.map(t -> new CreateWorkflowCommand.TransitionCommand(Label.of(t.label()), t.description(), t.mainFlow(),
				t.sourceTempKey(), t.targetTempKey()))
			.toList();

		return CreateWorkflowCommand.builder()
			.workspaceKey(workspaceKey)
			.label(Label.of(label))
			.description(description)
			.statuses(statusCommands)
			.transitions(transitionCommands)
			.build();
	}
}
