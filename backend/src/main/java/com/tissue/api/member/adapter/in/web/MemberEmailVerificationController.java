package com.tissue.api.member.adapter.in.web;

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
import com.tissue.api.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.api.member.application.service.MemberEmailVerificationService;
import com.tissue.api.member.adapter.in.web.dto.request.EmailVerificationRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/members/verification")
@RequiredArgsConstructor
public class MemberEmailVerificationController {

	private final MemberEmailVerificationService memberEmailVerificationService;
	private final EmailVerificationProperties properties;

	// TODO: URI 설계를 제대로 한건지 잘 모르겠네
	@PostMapping("/email-request")
	public ApiResponse<Void> request(@RequestBody @Valid EmailVerificationRequest request) {
		memberEmailVerificationService.sendVerificationEmail(request.email());
		return ApiResponse.okWithNoContent("Verification email sent.");
	}

	// TODO: 아니 이메일 인증 REST API인데 redirectUrl이 필요한가? 단순히 성공 or 실패 여부만 알면되는거 아닌가?
	@GetMapping("/email-verify")
	public ResponseEntity<Void> verifyEmail(
		@RequestParam String email,
		@RequestParam String token
	) {
		boolean verified = memberEmailVerificationService.verifyEmail(email, token);

		String redirectUrl = verified
			? properties.getSuccessUrl() : properties.getFailureUrl();

		return ResponseEntity
			.status(HttpStatus.FOUND)
			.header(HttpHeaders.LOCATION, redirectUrl)
			.build();

		// catch (InvalidRequestException e) {
		// 	return ResponseEntity
		// 		.status(HttpStatus.FOUND)
		// 		.header(HttpHeaders.LOCATION, properties.getFailureUrl())
		// 		.build();
		// }
	}

	@GetMapping("/email-status")
	public ApiResponse<Boolean> isVerified(@RequestParam String email) {
		boolean verified = memberEmailVerificationService.isEmailVerified(email);
		return ApiResponse.ok("Email verification status: " + verified, verified);
	}
}
