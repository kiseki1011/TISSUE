package com.tissue.workspace.adapter.in.web.dto.request;

import com.tissue.workspace.domain.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull WorkspaceRole role) {}
