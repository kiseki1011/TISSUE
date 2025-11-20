package com.tissue.api.workspace.adapter.in.web.dto.request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record InviteMembersRequest(
	@NotEmpty Set<@Email @NotBlank String> emails
) {
}
