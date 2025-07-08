package com.tissue.api.security.authentication.presentation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.member.presentation.dto.request.PermissionRequest;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.application.service.AuthenticationService;
import com.tissue.api.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.api.security.authentication.presentation.dto.request.RefreshTokenRequest;
import com.tissue.api.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.api.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.api.security.authentication.presentation.dto.response.RefreshTokenResponse;
import com.tissue.api.security.authentication.resolver.CurrentMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationService authenticationService;

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(
		@Valid @RequestBody LoginRequest request
	) {
		LoginResponse response = authenticationService.login(request);
		return ApiResponse.ok("Login successful.", response);
	}

	@PostMapping("/token")
	public ApiResponse<RefreshTokenResponse> refreshToken(
		@RequestBody RefreshTokenRequest request
	) {
		RefreshTokenResponse response = authenticationService.refreshToken(request);
		return ApiResponse.ok("Token refreshed", response);
	}

	@PostMapping("/token/elevate")
	public ApiResponse<ElevatedTokenResponse> elevatePermission(
		@RequestBody @Valid PermissionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		ElevatedTokenResponse response = authenticationService.elevatePermission(
			request,
			userDetails.getLoginId(),
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Permission granted.", response);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		// Todo: implement token blacklisting if needed!
		return ApiResponse.okWithNoContent("Logout successful.");
	}
}
