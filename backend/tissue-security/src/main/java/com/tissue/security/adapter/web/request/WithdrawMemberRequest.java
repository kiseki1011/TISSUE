package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record WithdrawMemberRequest(
        @Schema(description = "Current password for confirmation (required in LOCAL auth mode; ignored in OIDC mode)")
        @Nullable
        @Size(max = 100)
        String password) {}
