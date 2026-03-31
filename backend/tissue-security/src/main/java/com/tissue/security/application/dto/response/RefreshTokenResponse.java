package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refresh token response containing new JWT tokens")
public record RefreshTokenResponse(
        @Schema(description = "Refreshed JWT access token") String accessToken,
        @Schema(description = "Refreshed JWT refresh token") String refreshToken) {}
