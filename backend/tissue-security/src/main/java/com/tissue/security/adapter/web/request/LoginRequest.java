package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request")
public record LoginRequest(
        @Schema(
                description = "Email or username depending on server's `email-required` setting",
                example = "user@example.com")
        @NotBlank
        String identifier,

        @Schema(description = "Account password", example = "password1234!") @NotBlank
        String password) {}
