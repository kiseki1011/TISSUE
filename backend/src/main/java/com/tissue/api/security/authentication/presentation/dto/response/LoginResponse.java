package com.tissue.api.security.authentication.presentation.dto.response;

import lombok.Builder;

@Builder
public record LoginResponse(
	String accessToken,
	String refreshToken
) {
	public static LoginResponse from(String accessToken, String refreshToken) {
		return LoginResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}
}
