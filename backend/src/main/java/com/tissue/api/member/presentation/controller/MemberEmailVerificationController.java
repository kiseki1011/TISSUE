package com.tissue.api.member.presentation.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.member.application.service.command.MemberEmailVerificationService;
import com.tissue.api.member.presentation.dto.request.EmailVerificationRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members/email-verification")
@RequiredArgsConstructor
public class MemberEmailVerificationController {

	private final MemberEmailVerificationService memberEmailVerificationService;

	@PostMapping("/request")
	public ApiResponse<Void> request(@RequestBody @Valid EmailVerificationRequest request) {
		memberEmailVerificationService.sendVerificationEmail(request.email());
		return ApiResponse.okWithNoContent("Verification email sent.");
	}

	@GetMapping("/api/v1/members/verify")
	public ResponseEntity<Void> verifyEmail(
		@RequestParam String email,
		@RequestParam String token
	) {
		try {
			boolean verified = memberEmailVerificationService.verifyEmail(email, token);

			String redirectUrl = verified
				? "https://yourdomain.com/email/verify/success"
				: "https://yourdomain.com/email/verify/fail";

			return ResponseEntity
				.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, redirectUrl)
				.build();

		} catch (InvalidRequestException e) {
			return ResponseEntity
				.status(HttpStatus.FOUND)
				.header(HttpHeaders.LOCATION, "https://yourdomain.com/email/verify/fail")
				.build();
		}
	}
}
