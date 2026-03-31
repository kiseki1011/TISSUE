package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Password reset email request")
public record PasswordResetRequest(
        @Schema(description = "Email address associated with the account", example = "user@example.com")
        @NotBlank
        @Email
        String email) {}
