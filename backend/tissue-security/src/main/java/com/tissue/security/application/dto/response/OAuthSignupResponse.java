package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "OAuth signup response containing JWT tokens")
@Builder
public record OAuthSignupResponse(
        @Schema(description = "JWT access token") String accessToken,
        @Schema(description = "JWT refresh token") String refreshToken) {}
