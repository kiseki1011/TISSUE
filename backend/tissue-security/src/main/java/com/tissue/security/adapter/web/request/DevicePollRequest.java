package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Poll an OIDC device login that is in progress")
public record DevicePollRequest(
        @Schema(description = "The device code returned by device:start") @NotBlank
        String deviceCode) {}
