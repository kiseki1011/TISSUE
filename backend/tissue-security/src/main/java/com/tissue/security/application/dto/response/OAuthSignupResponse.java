package com.tissue.security.application.dto.response;

import lombok.Builder;

@Builder
public record OAuthSignupResponse(String accessToken, String refreshToken) {}
