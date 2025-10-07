package com.tissue.api.issue.workflow.presentation.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.util.JsonNullables;
import com.tissue.api.issue.base.domain.model.vo.Label;
import com.tissue.api.issue.workflow.application.dto.PatchTransitionCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatchTransitionRequest(
	JsonNullable<@NotBlank @Size(max = 32) String> label,
	JsonNullable<@Size(max = 255) String> description
) {
	public PatchTransitionCommand toCommand(String workspaceKey, Long workflowId, Long transitionId) {
		return PatchTransitionCommand.builder()
			.workspaceKey(workspaceKey)
			.workflowId(workflowId)
			.transitionId(transitionId)
			.label(JsonNullables.map(label, Label::of))
			.description(description)
			.build();
	}
}
