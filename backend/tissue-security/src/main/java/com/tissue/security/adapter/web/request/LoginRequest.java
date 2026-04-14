package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(
                description = "Email or username depending on server's `email-required` setting",
                example = "gildong@termissue.dev")
        @NotBlank
        @Size(max = 255)
        String identifier,

        @NotBlank @Size(max = 100) String password) {}
