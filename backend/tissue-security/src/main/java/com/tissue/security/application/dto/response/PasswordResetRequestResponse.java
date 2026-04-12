package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PasswordResetRequestResponse(
        @Schema(description = "Verification ID for polling the password reset status")
        String verificationId) {}
