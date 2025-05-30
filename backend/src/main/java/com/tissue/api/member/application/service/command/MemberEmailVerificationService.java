package com.tissue.api.member.application.service.command;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.email.domain.EmailClient;
import com.tissue.api.member.config.EmailVerificationProperties;
import com.tissue.api.member.domain.repository.verification.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

	private final EmailVerificationRepository repository;
	private final EmailClient emailClient;
	private final EmailVerificationProperties properties;

	// TODO: @ConfigurationProperties(prefix = "email.verification")를 사용해서 TTL 값 관리
	private static final Duration TTL = Duration.ofMinutes(30);

	// TODO: 도메인을 포함한 스트링값들 하드 코딩하지 않기
	// TODO: 이메일 포맷에 thymeleaf 사용
	public void sendVerificationEmail(String email) {
		String tokenValue = UUID.randomUUID().toString();
		repository.saveToken(email, tokenValue, TTL);

		String link = properties.getVerificationUrl() + "?email=%s&token=%s"
			.formatted(email, tokenValue);

		String subject = "Tissue - Email Verification";
		String content = """
			Hello,

			Please verify your email address by clicking the link below:

			%s

			This link is valid for 30 minutes.

			- Tissue Team
			""".formatted(link);

		emailClient.send(email, subject, content);
	}

	public boolean verifyEmail(String email, String tokenValue) {
		return repository.verify(email, tokenValue);
	}

	public void validateEmailVerified(String email) {
		boolean emailNotVerified = !repository.isVerified(email);
		if (emailNotVerified) {
			throw new InvalidRequestException("Email is not verified. Please complete verification before signing up.");
		}
	}

	public boolean isEmailVerified(String email) {
		return repository.isVerified(email);
	}
}
