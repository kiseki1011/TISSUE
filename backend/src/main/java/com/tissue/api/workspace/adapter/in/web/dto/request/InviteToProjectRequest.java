package com.tissue.api.workspace.adapter.in.web.dto.request;

import java.util.Set;

import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.workspace.application.dto.request.InviteToProjectCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record InviteToProjectRequest(
	@NotEmpty Set<@Email @NotBlank String> emails,
	@NotNull ProjectRole role
) {
	public InviteToProjectCommand toCommand(String workspaceKey, String projectKey) {
		return new InviteToProjectCommand(
			emails,
			workspaceKey,
			projectKey,
			role
		);
	}
}
