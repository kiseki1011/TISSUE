package com.tissue.project.adapter.in.web.dto.request;

import com.tissue.project.domain.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ChangeProjectRoleRequest(@NotNull ProjectRole grantRole) {}
