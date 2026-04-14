package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.domain.RefreshToken;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class RdbRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    @Override
    @Transactional
    public void save(Long memberId, String refreshToken, Duration ttl) {
        jpaRepository.deleteByMemberId(memberId);
        jpaRepository.save(RefreshToken.create(memberId, refreshToken, ttl));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findByMemberId(Long memberId) {
        return jpaRepository
                .findByMemberId(memberId)
                .filter(t -> !t.isExpired())
                .map(RefreshToken::getHashedToken);
    }

    @Override
    @Transactional
    public void deleteByMemberId(Long memberId) {
        jpaRepository.deleteByMemberId(memberId);
    }
}
