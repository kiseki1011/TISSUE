package com.tissue.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(
        name = "email_verification_token",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_email_verification_token", columnNames = "email")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    // TODO: is there a way to enforce to set all fields? instead of using a AllArgsConstructor?
    public static EmailVerificationToken create(
            @NonNull String email, @NonNull String tokenValue, @NonNull Duration ttl) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.email = email;
        token.tokenValue = tokenValue;
        token.verified = false;
        token.expiresAt = Instant.now().plus(ttl);

        return token;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean tokenValueNotMatch(String tokenValue) {
        return !Objects.equals(this.tokenValue, tokenValue);
    }

    public void markVerified() {
        this.verified = true;
    }
}
