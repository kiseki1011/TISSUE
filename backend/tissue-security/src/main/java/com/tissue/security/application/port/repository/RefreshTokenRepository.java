package com.tissue.security.application.port.repository;

import java.time.Duration;
import java.util.Optional;

public interface RefreshTokenRepository {

    void save(Long memberId, String refreshToken, Duration ttl);

    Optional<String> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
