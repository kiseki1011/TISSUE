package com.tissue.api.workflow.adapter.in.web.dto.request;

import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.workflow.application.dto.EntityRef;
import com.tissue.api.workflow.application.dto.StateDefinition;
import com.tissue.api.workflow.application.dto.TransitionDefinition;
import com.tissue.api.workflow.application.dto.request.CreateWorkflowCommand;

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
		@NotNull StateCategory category
	) {
	}

	public record CreateTransitionRequest(
		@NotBlank @Size(max = 32) String label,
		@Nullable @Size(max = 255) String description,
		@NotBlank String sourceTempKey,
		@NotBlank String targetTempKey
	) {
	}

	public CreateWorkflowCommand toCommand(String workspaceKey, String projectKey) {
		List<StateDefinition> stateDefinitions = createStatusRequests.stream()
			.map(s -> new StateDefinition(
				new EntityRef(null, s.tempKey()),
				Label.of(s.label()),
				s.description(),
				s.color(),
				s.category
			))
			.toList();

		List<TransitionDefinition> transitionCommands = createTransitionRequests.stream()
			.map(t -> new TransitionDefinition(
				null,
				Label.of(t.label()),
				t.description(),
				new EntityRef(null, t.sourceTempKey()),
				new EntityRef(null, t.targetTempKey())
			))
			.toList();

		return CreateWorkflowCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.label(Label.of(label))
			.description(description)
			.color(color)
			.stateDefinitions(stateDefinitions)
			.transitionDefinitions(transitionCommands)
			.build();
	}
}
