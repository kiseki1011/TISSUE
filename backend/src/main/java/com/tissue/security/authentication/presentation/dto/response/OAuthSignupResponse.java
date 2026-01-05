package com.tissue.security.authentication.presentation.dto.response;

import lombok.Builder;

@Builder
public record OAuthSignupResponse(String accessToken, String refreshToken) {}
