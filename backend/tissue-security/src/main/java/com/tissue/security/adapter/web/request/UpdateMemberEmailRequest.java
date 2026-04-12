package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberEmailRequest(
        @NotBlank @Email @Size(min = 4, max = 255) String newEmail,

        @Schema(description = "Email verification token") @NotBlank
        String verificationToken) {}
