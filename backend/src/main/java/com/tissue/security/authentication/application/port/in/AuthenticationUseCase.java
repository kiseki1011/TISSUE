package com.tissue.security.authentication.application.port.in;

import com.tissue.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.security.authentication.presentation.dto.response.RefreshTokenResponse;

public interface AuthenticationUseCase {

	LoginResponse login(String loginEmail, String password);

	RefreshTokenResponse refreshToken(String refreshToken);

	ElevatedTokenResponse elevatePermission(String loginEmail, String password, Long memberId);
}
