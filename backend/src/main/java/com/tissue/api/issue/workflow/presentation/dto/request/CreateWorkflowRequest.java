package com.tissue.api.issue.workflow.presentation.dto.request;

import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.domain.service.WorkflowGraphValidator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(
	@NotBlank @Size(max = 32) String label,
	@Nullable @Size(max = 255) String description,
	@NotEmpty List<CreateStatusRequest> createStatusRequests,
	@NotEmpty List<CreateTransitionRequest> createTransitionRequests
) {
	public record CreateStatusRequest(
		@NotBlank String tempKey,
		@NotBlank @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotNull boolean initial,
		@NotNull boolean terminal
	) {
	}

	public record CreateTransitionRequest(
		@NotBlank @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotNull boolean mainFlow,
		@NotBlank String sourceTempKey,
		@NotBlank String targetTempKey
	) {
	}

	public CreateWorkflowCommand toCommand(String workspaceKey) {
		List<CreateWorkflowCommand.CreateStatusCommand> createStatusCommands = createStatusRequests.stream()
			.map(s -> new CreateWorkflowCommand.CreateStatusCommand(
				new WorkflowGraphValidator.EntityRef(null, s.tempKey()),
				Label.of(s.label()),
				s.description(),
				s.initial(),
				s.terminal()
			))
			.toList();

		List<CreateWorkflowCommand.CreateTransitionCommand> createTransitionCommands = createTransitionRequests.stream()
			.map(t -> new CreateWorkflowCommand.CreateTransitionCommand(
				Label.of(t.label()),
				t.description(),
				t.mainFlow(),
				new WorkflowGraphValidator.EntityRef(null, t.sourceTempKey()),
				new WorkflowGraphValidator.EntityRef(null, t.targetTempKey())
			))
			.toList();

		return CreateWorkflowCommand.builder()
			.workspaceKey(workspaceKey)
			.label(Label.of(label))
			.description(description)
			.createStatusCommands(createStatusCommands)
			.createTransitionCommands(createTransitionCommands)
			.build();
	}
}
