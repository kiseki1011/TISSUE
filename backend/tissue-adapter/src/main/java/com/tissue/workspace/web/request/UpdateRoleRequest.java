package com.tissue.workspace.web.request;

import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull WorkspaceRole role) {}
