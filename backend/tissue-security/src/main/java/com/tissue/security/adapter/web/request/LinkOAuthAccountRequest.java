package com.tissue.security.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Link OAuth account request")
public record LinkOAuthAccountRequest(
        @Schema(description = "Register token from OAuth callback") @NotBlank
        String registerToken) {}
