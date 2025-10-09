package com.tissue.api.workflow.presentation.dto.request;

import org.openapitools.jackson.nullable.JsonNullable;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.util.JsonNullables;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.workflow.application.dto.PatchWorkflowCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatchWorkflowRequest(
	JsonNullable<@NotBlank @Size(max = 32) String> label,
	JsonNullable<@Size(max = 255) String> description,
	JsonNullable<@NotNull ColorType> color
) {
	public PatchWorkflowCommand toCommand(String workspaceKey, Long id) {
		return PatchWorkflowCommand.builder()
			.workspaceKey(workspaceKey)
			.id(id)
			.label(JsonNullables.map(label, Label::of))
			.description(description)
			.color(color)
			.build();
	}
}
