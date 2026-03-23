package com.tissue.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "email_verification_token",
        uniqueConstraints = {@UniqueConstraint(name = "uk_email_verification_token", columnNames = "email")})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String tokenValue;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private String verificationId;

    private String verifiedToken;

    @SuppressWarnings("NullAway.Init")
    protected EmailVerificationToken() {}

    public static EmailVerificationToken create(String email, String tokenValue, Duration ttl, String verificationId) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.email = email;
        token.tokenValue = tokenValue;
        token.verified = false;
        token.expiresAt = Instant.now().plus(ttl);
        token.verificationId = verificationId;

        return token;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void markVerified(String verifiedToken, Duration ttl) {
        this.verified = true;
        this.verifiedToken = verifiedToken;
        this.expiresAt = Instant.now().plus(ttl);
    }
}
