package com.tissue.api.member.infrastructure.verification.rdb;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidRequestException;
import com.tissue.api.member.domain.repository.verification.EmailVerificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "email.verification.strategy", havingValue = "rdb", matchIfMissing = true)
@RequiredArgsConstructor
public class RdbEmailVerificationRepository implements EmailVerificationRepository {

	private final EmailVerificationJpaRepository jpaRepository;

	@Override
	@Transactional
	public void saveToken(String email, String tokenValue, Duration ttl) {
		EmailVerificationToken verificationToken = jpaRepository.findByEmail(email)
			.map(t -> {
				t.markVerified(); // 이전 토큰 무효화
				return EmailVerificationToken.create(email, tokenValue, ttl);
			})
			.orElse(EmailVerificationToken.create(email, tokenValue, ttl));

		try {
			jpaRepository.save(verificationToken);
		} catch (DataIntegrityViolationException e) {
			log.warn("Duplicate verification token for email: {}", email, e);
			throw new InvalidRequestException("A verification email was already sent. Please try again shortly.");
		}
	}

	@Override
	@Transactional
	public boolean verify(String email, String tokenValue) {
		EmailVerificationToken token = jpaRepository.findByEmail(email)
			.orElseThrow(() -> new InvalidRequestException("Invalid token"));

		if (token.isExpired() || token.tokenValueNotMatch(tokenValue)) {
			return false;
		}

		token.markVerified();
		return true;
	}

	@Override
	public boolean isVerified(String email) {
		return jpaRepository.findByEmail(email)
			.map(t -> t.isVerified() && !t.isExpired())
			.orElse(false);
	}

	@Override
	@Transactional
	public void deleteToken(String email) {
		jpaRepository.deleteByEmail(email);
	}
}
