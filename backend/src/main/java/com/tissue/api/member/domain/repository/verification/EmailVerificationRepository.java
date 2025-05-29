package com.tissue.api.member.domain.repository.verification;

import java.time.Duration;

public interface EmailVerificationRepository {
	void saveToken(String email, String token, Duration ttl);

	// TODO: void 반환으로 바꾸고, throws InvalidRequestException으로 하는게 좋을까?
	boolean verify(String email, String token);

	boolean isVerified(String email);
}
