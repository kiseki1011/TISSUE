package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Signup email verification response")
public record SignupVerificationResponse(
        @Schema(description = "Verification ID for polling verification status")
        String verificationId) {}
