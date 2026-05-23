package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials for an account restore request")
public record RestoreMemberRequest(
        @Schema(
                description = "Login identifier: email when `email-required` is enabled, otherwise username",
                example = "gildong@try-tissue.dev")
        @NotBlank
        @Size(max = 320)
        String identifier,

        @Schema(description = "Password matching the authentication identity") @NotBlank @Size(max = 100)
        String password) {}
