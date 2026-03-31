package com.tissue.security.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Login response containing JWT tokens")
@Builder
public record LoginResponse(
        @Schema(description = "JWT access token") String accessToken,
        @Schema(description = "JWT refresh token") String refreshToken) {
    public static LoginResponse from(String accessToken, String refreshToken) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
