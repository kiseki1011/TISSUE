package com.tissue.authentication.application.port.in;

import com.tissue.authentication.application.dto.response.ElevatedTokenResponse;
import com.tissue.authentication.application.dto.response.LoginResponse;
import com.tissue.authentication.application.dto.response.RefreshTokenResponse;

public interface AuthenticationUseCase {

    LoginResponse login(String loginEmail, String password);

    RefreshTokenResponse refreshToken(String refreshToken);

    ElevatedTokenResponse elevatePermission(String loginEmail, String password);

    void logout(String email);
}
