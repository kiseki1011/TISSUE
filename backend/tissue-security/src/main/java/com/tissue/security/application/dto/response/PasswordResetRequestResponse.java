package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Password reset request response")
public record PasswordResetRequestResponse(
        @Schema(description = "Verification ID for polling reset status")
        String verificationId) {}
