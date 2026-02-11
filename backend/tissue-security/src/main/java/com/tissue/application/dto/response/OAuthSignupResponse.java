package com.tissue.application.dto.response;

import lombok.Builder;

@Builder
public record OAuthSignupResponse(String accessToken, String refreshToken) {}
