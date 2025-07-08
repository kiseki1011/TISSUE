package com.tissue.api.security.authentication.application.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.presentation.dto.request.PermissionRequest;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.MemberUserDetailsService;
import com.tissue.api.security.authentication.jwt.JwtTokenService;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.request.RefreshTokenRequest;
import com.tissue.api.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.api.security.authentication.presentation.dto.response.RefreshTokenResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenService jwtTokenService;
	private final MemberUserDetailsService userDetailsService;

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

	@Transactional
	public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {

		String refreshToken = request.refreshToken();

		// validate refresh token
		jwtTokenService.validateRefreshToken(refreshToken);

		// extract subject (login identifier)
		String loginIdentifier = jwtTokenService.getSubjectFromToken(refreshToken);

		// load user to ensure they still exist and are valid
		MemberUserDetails userDetails = (MemberUserDetails)userDetailsService.loadUserByUsername(loginIdentifier);

		// create new access token
		String newAccessToken = jwtTokenService.createAccessToken(userDetails.getMemberId(), userDetails.getLoginId());

		return new RefreshTokenResponse(newAccessToken);
	}

	@Transactional
	public ElevatedTokenResponse elevatePermission(PermissionRequest request, String loginIdentifier, Long memberId) {

		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(loginIdentifier, request.password())
		);

		String elevatedToken = jwtTokenService.createElevatedToken(memberId, loginIdentifier);

		return new ElevatedTokenResponse(elevatedToken);
	}
}
