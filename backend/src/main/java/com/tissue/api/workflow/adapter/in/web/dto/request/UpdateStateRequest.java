package com.tissue.api.workflow.adapter.in.web.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.util.JsonNullables;
import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.application.dto.request.UpdateStateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStateRequest(
	JsonNullable<@NotBlank @Size(max = 32) String> label,
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<@NotNull ColorType> color
) {
	public UpdateStateCommand toCommand(String workspaceKey, String projectKey, Long workflowId, Long stateId) {
		return UpdateStateCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.workflowId(workflowId)
			.stateId(stateId)
			.label(JsonNullables.map(label, Label::of))
			.description(description)
			.color(color)
			.build();
	}
}
