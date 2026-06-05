package com.tissue.security.domain;

import com.tissue.feature.member.domain.Member;
import com.tissue.shared.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * An opaque token a member uses to authenticate (the MCP server is the first consumer).
 *
 * <p>Only the SHA-256 hash is stored.Tthe raw token is shown once at token creation.
 * Lookups are by hash.
 */
@Entity
@Getter
@Table(
        name = "personal_access_token",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_pat_token_hash", columnNames = "token_hash"),
            @UniqueConstraint(
                    name = "uk_pat_member_name",
                    columnNames = {"member_id", "name"})
        })
public class PersonalAccessToken extends BaseDateEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String name;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PatScope scope;

    /**
     * If {@code null}, token never expires.
     */
    @Nullable
    private Instant expiresAt;

    @Nullable
    private Instant lastUsedAt;

    @Column(nullable = false)
    private boolean revoked;

    @Nullable
    private Instant revokedAt;

    @SuppressWarnings("NullAway.Init")
    protected PersonalAccessToken() {}

    public static PersonalAccessToken create(
            Member member, String name, String tokenHash, PatScope scope, @Nullable Duration ttl) {
        PersonalAccessToken token = new PersonalAccessToken();
        token.member = member;
        token.name = name;
        token.tokenHash = tokenHash;
        token.scope = scope;
        token.expiresAt = (ttl == null) ? null : Instant.now().plus(ttl);
        token.revoked = false;
        return token;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    /**
     * Records that the token was used, but only when the previous record is older than {@code throttle}.
     */
    public void touchIfStale(Duration throttle) {
        Instant now = Instant.now();
        if (lastUsedAt == null || lastUsedAt.plus(throttle).isBefore(now)) {
            this.lastUsedAt = now;
        }
    }
}
