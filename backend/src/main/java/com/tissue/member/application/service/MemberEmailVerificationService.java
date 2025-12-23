package com.tissue.member.application.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tissue.email.domain.EmailClient;
import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import com.tissue.member.domain.exception.MemberExceptions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

	// TODO: use a property class to get value from application.yml
	private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

	private final EmailVerificationRepository repository;
	private final EmailClient emailClient;
	private final EmailVerificationProperties properties;

	public void sendVerificationEmail(String email) {
		String tokenValue = UUID.randomUUID().toString();
		repository.saveToken(email, tokenValue, DEFAULT_TTL);

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
			throw MemberExceptions.emailNotVerified(email);
		}
	}

	public boolean isEmailVerified(String email) {
		return repository.isVerified(email);
	}

	public void clearVerification(String email) {
		repository.deleteToken(email);
	}
}
