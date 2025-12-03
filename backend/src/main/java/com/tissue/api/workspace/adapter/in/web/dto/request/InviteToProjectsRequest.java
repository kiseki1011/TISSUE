package com.tissue.api.workspace.adapter.in.web.dto.request;

import java.util.Set;

import com.tissue.api.workspace.application.dto.ProjectJoinConfigDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record InviteToProjectsRequest(
	@NotEmpty Set<@Email @NotBlank String> emails,
	@NotEmpty Set<ProjectJoinConfigDto> targetProjects
) {
	// TODO: toCommand
}
