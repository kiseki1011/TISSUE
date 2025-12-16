package com.tissue.workflow.adapter.in.web.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.common.util.JsonNullables;
import com.tissue.common.vo.Label;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTransitionRequest(
	JsonNullable<@NotBlank @Size(max = 32) String> label,
	JsonNullable<@Size(max = 255) String> description
) {
	public UpdateTransitionCommand toCommand(String workspaceKey, String projectKey, Long workflowId,
		Long transitionId) {
		return UpdateTransitionCommand.builder()
			.workspaceKey(workspaceKey)
			.projectKey(projectKey)
			.workflowId(workflowId)
			.transitionId(transitionId)
			.label(JsonNullables.map(label, Label::of))
			.description(description)
			.build();
	}
}
