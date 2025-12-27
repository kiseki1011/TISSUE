package com.tissue.position.adapter.in.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.position.application.dto.request.CreatePositionCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
	@NotBlank
	@Size(max = 100)
	String name,

	@Size(max = 255)
	String description,

	@NotNull
	ColorType color
) {
	public CreatePositionCommand toCommand(String workspaceKey) {
		return new CreatePositionCommand(workspaceKey, name, description, color);
	}
}
