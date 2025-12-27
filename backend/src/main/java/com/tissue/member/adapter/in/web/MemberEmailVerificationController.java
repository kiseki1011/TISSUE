package com.tissue.member.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.adapter.in.web.dto.request.EmailVerificationRequest;
import com.tissue.member.application.service.MemberEmailVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members/verification")
@RequiredArgsConstructor
public class MemberEmailVerificationController {

	private final MemberEmailVerificationService memberEmailVerificationService;
	private final EmailVerificationProperties properties;

	@PostMapping("/request")
	public ResponseEntity<Void> requestVerification(@RequestBody @Valid EmailVerificationRequest request) {
		memberEmailVerificationService.sendVerificationEmail(request.email());
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/verify")
	public ResponseEntity<Void> verifyEmail(
		@RequestParam String email,
		@RequestParam String token
	) {
		boolean verified = memberEmailVerificationService.verifyEmail(email, token);

		// TODO: what is the use for the redirect url?
		//  send the user to the designated url after the user clicks the verification link?
		//  in this case shouldnt i create a thymeleaf page for each url?
		String redirectUrl = verified
			? properties.getSuccessUrl() : properties.getFailureUrl();

		return ResponseEntity
			.status(HttpStatus.FOUND)
			.header(HttpHeaders.LOCATION, redirectUrl)
			.build();
	}

	@GetMapping("/verifyStatus")
	public ResponseEntity<Boolean> checkVerification(@RequestParam String email) {
		boolean verified = memberEmailVerificationService.isEmailVerified(email);
		return ResponseEntity.ok(verified);
	}
}
