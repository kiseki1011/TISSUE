package com.tissue.workspace.adapter.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record InviteToProjectRequest(@NotEmpty Set<@Email @NotBlank String> emails) {}
