package com.tissue.admin.adapter.web.request;

import com.tissue.feature.member.domain.SystemRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeSystemRoleRequest(
        @Schema(description = "New system role to assign", example = "ADMIN") @NotNull
        SystemRole role) {}
