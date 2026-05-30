package com.tissue.security.application.port.usecase;

import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;

public interface AuthenticationUseCase {

    LoginResponse login(String identifier, String password, String clientIp);

    RefreshTokenResponse refreshToken(String refreshToken);

    void logout(Long memberId);
}
