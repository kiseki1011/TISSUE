package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Account withdrawal request")
public record WithdrawMemberRequest(
        @Schema(description = "Current password for confirmation") @NotBlank @Size(max = 100)
        String password) {}
