package com.tissue.security.domain;

import com.tissue.shared.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "refresh_token",
        uniqueConstraints = {@UniqueConstraint(name = "uk_refresh_token_member_id", columnNames = "member_id")})
public class RefreshToken extends BaseDateEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 1000)
    private String hashedToken;

    @Column(nullable = false)
    private Instant expiresAt;

    @SuppressWarnings("NullAway.Init")
    protected RefreshToken() {}

    public static RefreshToken create(Long memberId, String hashedToken, Duration ttl) {
        RefreshToken token = new RefreshToken();
        token.memberId = memberId;
        token.hashedToken = hashedToken;
        token.expiresAt = Instant.now().plus(ttl);
        return token;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
