package com.tissue.member.application.port.out;

import java.time.Duration;

public interface EmailVerificationRepository {

    void saveToken(String email, String token, Duration ttl);

    boolean verify(String email, String token);

    boolean isVerified(String email);

    void deleteToken(String email);
}
