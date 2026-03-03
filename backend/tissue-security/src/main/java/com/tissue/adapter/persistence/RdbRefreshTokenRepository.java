package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.RefreshTokenRepository;
import com.tissue.domain.RefreshToken;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "tissue.security.refresh-token.store", havingValue = "rdb", matchIfMissing = true)
@RequiredArgsConstructor
public class RdbRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    @Transactional
    public void save(String email, String refreshToken, Duration ttl) {
        jpaRepository.deleteByEmail(email);
        jpaRepository.save(RefreshToken.create(email, refreshToken, ttl));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findByEmail(String email) {
        return jpaRepository.findByEmail(email).filter(t -> !t.isExpired()).map(RefreshToken::getTokenValue);
    }

    @Override
    @Transactional
    public void deleteByEmail(String email) {
        jpaRepository.deleteByEmail(email);
    }
}
