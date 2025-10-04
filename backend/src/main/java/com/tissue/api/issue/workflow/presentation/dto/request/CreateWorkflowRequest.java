package com.tissue.api.issue.workflow.presentation.dto.request;

import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.CreateWorkflowCommand;
import com.tissue.api.issue.workflow.domain.service.EntityRef;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(
	@NotBlank @Size(max = 32) String label,
	@Nullable @Size(max = 255) String description,
	@NotNull ColorType color,
	@NotEmpty List<CreateStatusRequest> createStatusRequests,
	@NotEmpty List<CreateTransitionRequest> createTransitionRequests
) {
	public record CreateStatusRequest(
		@NotBlank String tempKey,
		@NotBlank @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotNull ColorType color,
		@NotNull boolean initial,
		@NotNull boolean terminal
	) {
	}

	public record CreateTransitionRequest(
		@NotBlank @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotBlank String sourceTempKey,
		@NotBlank String targetTempKey
	) {
	}

	public CreateWorkflowCommand toCommand(String workspaceKey) {
		List<CreateWorkflowCommand.StatusCommand> statusCommands = createStatusRequests.stream()
			.map(s -> new CreateWorkflowCommand.StatusCommand(
				new EntityRef(null, s.tempKey()),
				Label.of(s.label()),
				s.description(),
				s.color(),
				s.initial(),
				s.terminal()
			))
			.toList();

		List<CreateWorkflowCommand.TransitionCommand> transitionCommands = createTransitionRequests.stream()
			.map(t -> new CreateWorkflowCommand.TransitionCommand(
				Label.of(t.label()),
				t.description(),
				new EntityRef(null, t.sourceTempKey()),
				new EntityRef(null, t.targetTempKey())
			))
			.toList();

		return CreateWorkflowCommand.builder()
			.workspaceKey(workspaceKey)
			.label(Label.of(label))
			.description(description)
			.color(color)
			.statusCommands(statusCommands)
			.transitionCommands(transitionCommands)
			.build();
	}
}
