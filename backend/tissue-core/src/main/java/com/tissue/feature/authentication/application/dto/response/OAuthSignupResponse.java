package com.tissue.feature.authentication.application.dto.response;

import lombok.Builder;

@Builder
public record OAuthSignupResponse(String accessToken, String refreshToken) {}
