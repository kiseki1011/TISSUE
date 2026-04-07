package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Password reset email request")
public record PasswordResetRequest(
        @Schema(description = "The member account's email address", example = "gildong@termissue.dev") @NotBlank @Email
        String email) {}
