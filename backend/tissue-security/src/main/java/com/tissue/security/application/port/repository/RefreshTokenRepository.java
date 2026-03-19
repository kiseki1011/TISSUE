package com.tissue.security.application.port.repository;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(String email, String refreshToken, Duration ttl);

    Optional<String> findByEmail(String email);

    void deleteByEmail(String email);
}
