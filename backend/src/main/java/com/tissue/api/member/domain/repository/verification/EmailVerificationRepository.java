package com.tissue.api.member.domain.repository.verification;

import java.time.Duration;

public interface EmailVerificationRepository {
	void saveToken(String email, String token, Duration ttl);

	boolean verify(String email, String token);

	boolean isVerified(String email);

	void deleteToken(String email);
}
