package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Elevated token response for sensitive operations")
public record ElevatedTokenResponse(
        @Schema(description = "Short-lived elevated JWT token")
        String elevatedToken) {}
