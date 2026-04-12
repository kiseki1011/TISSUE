package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupVerificationResponse(
        @Schema(description = "Verification ID for polling verification status")
        String verificationId) {}
