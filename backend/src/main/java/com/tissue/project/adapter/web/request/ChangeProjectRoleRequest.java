package com.tissue.project.adapter.web.request;

import com.tissue.project.domain.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ChangeProjectRoleRequest(@NotNull ProjectRole grantRole) {}
