package com.tissue.member.application.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.tissue.email.domain.EmailClient;
import com.tissue.member.adapter.in.web.config.EmailVerificationProperties;
import com.tissue.member.application.port.out.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;

// TODO: do i need to make a usecase interface for this?
@Service
@RequiredArgsConstructor
public class MemberEmailVerificationService {

	private final EmailVerificationRepository repository;
	private final EmailClient emailClient;
	private final EmailVerificationProperties properties;

	// TODO: use application.yml to get configuration value(do not hard code)
	private static final Duration TTL = Duration.ofMinutes(30);

	// TODO(later): improve email format to a better design
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
			// TODO: EmailExceptions.emailNotVerified
			throw new RuntimeException("Email is not verified. Please complete verification before signing up.");
		}
	}

	public boolean isEmailVerified(String email) {
		return repository.isVerified(email);
	}

	public void clearVerification(String email) {
		repository.deleteToken(email);
	}
}
