package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Email verification request")
public record EmailVerificationRequest(
        @Schema(description = "Email address to verify", example = "user@example.com")
        @NotBlank
        @Email
        @Size(min = 4, max = 255)
        String email) {}
