package com.tissue.security.application.port.usecase;

import com.tissue.security.application.dto.response.ElevatedTokenResponse;
import com.tissue.security.application.dto.response.LoginResponse;
import com.tissue.security.application.dto.response.RefreshTokenResponse;

public interface AuthenticationUseCase {

    LoginResponse login(String loginEmail, String password, String clientIp);

    RefreshTokenResponse refreshToken(String refreshToken);

    ElevatedTokenResponse elevatePermission(String loginEmail, String password);

    void logout(String email);
}
