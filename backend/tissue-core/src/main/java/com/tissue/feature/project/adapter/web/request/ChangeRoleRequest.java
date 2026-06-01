package com.tissue.feature.project.adapter.web.request;

import com.tissue.feature.project.domain.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull ProjectRole role) {}
