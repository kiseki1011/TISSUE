package com.tissue.api.security.authentication.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.jwt.JwtTokenService;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;

	@Transactional
	public LoginResponse login(LoginRequest request) {

		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.identifier(), request.password())
		);

		MemberUserDetails userDetails = (MemberUserDetails)authentication.getPrincipal();

		String accessToken = jwtTokenService.createAccessToken(userDetails.getMemberId(), userDetails.getLoginId());
		String refreshToken = jwtTokenService.createRefreshToken(userDetails.getLoginId());

		return LoginResponse.from(accessToken, refreshToken);
	}
}
