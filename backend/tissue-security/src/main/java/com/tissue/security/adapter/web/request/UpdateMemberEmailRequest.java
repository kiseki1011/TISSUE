package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Update email request")
public record UpdateMemberEmailRequest(
        @Schema(description = "New email address", example = "newemail@example.com")
        @NotBlank
        @Email
        @Size(min = 4, max = 255)
        String newEmail,

        @Schema(description = "Email verification token") @NotBlank
        String verificationToken) {}
