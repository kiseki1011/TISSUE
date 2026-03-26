package com.tissue.security.adapter.persistence;

import com.tissue.security.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends Repository<RefreshToken, Long> {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByMemberId(Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt <= :now")
    void deleteByExpiresAtBefore(@Param("now") Instant now);
}
