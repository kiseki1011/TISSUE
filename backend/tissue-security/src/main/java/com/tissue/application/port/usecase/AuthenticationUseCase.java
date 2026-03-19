package com.tissue.application.port.usecase;

import com.tissue.application.dto.response.ElevatedTokenResponse;
import com.tissue.application.dto.response.LoginResponse;
import com.tissue.application.dto.response.RefreshTokenResponse;

public interface AuthenticationUseCase {

    LoginResponse login(String loginEmail, String password, String clientIp);

    RefreshTokenResponse refreshToken(String refreshToken);

    ElevatedTokenResponse elevatePermission(String loginEmail, String password);

    void logout(String email);
}
