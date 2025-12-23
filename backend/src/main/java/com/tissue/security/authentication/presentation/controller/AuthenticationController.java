package com.tissue.security.authentication.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.member.adapter.in.web.dto.request.PermissionRequest;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.application.service.AuthenticationService;
import com.tissue.security.authentication.presentation.dto.request.LoginRequest;
import com.tissue.security.authentication.presentation.dto.request.RefreshTokenRequest;
import com.tissue.security.authentication.presentation.dto.response.ElevatedTokenResponse;
import com.tissue.security.authentication.presentation.dto.response.LoginResponse;
import com.tissue.security.authentication.presentation.dto.response.RefreshTokenResponse;
import com.tissue.security.authentication.resolver.CurrentMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

	private final AuthenticationService authenticationService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(
		@Valid @RequestBody LoginRequest request
	) {
		LoginResponse response = authenticationService.login(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/token")
	public ResponseEntity<RefreshTokenResponse> refreshToken(
		@RequestBody RefreshTokenRequest request
	) {
		RefreshTokenResponse response = authenticationService.refreshToken(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/token/elevate")
	public ResponseEntity<ElevatedTokenResponse> elevatePermission(
		@RequestBody @Valid PermissionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		ElevatedTokenResponse response = authenticationService.elevatePermission(
			request,
			userDetails.getEmail(),
			userDetails.getMemberId()
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		// Todo: implement token blacklisting if needed!
		return ResponseEntity.noContent().build();
	}
}
