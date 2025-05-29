package com.tissue.api.member.infrastructure.verification.rdb;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification_token", uniqueConstraints = {
	@UniqueConstraint(name = "UK_EMAIL_VERIFICATION_EMAIL", columnNames = "email")
})
public class EmailVerificationToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String tokenValue;

	@Column(nullable = false)
	private boolean verified = false;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	@Builder
	public EmailVerificationToken(String email, String tokenValue, boolean verified, LocalDateTime expiresAt) {
		this.email = email;
		this.tokenValue = tokenValue;
		this.verified = verified;
		this.expiresAt = expiresAt;
	}

	public static EmailVerificationToken create(String email, String tokenValue, Duration ttl) {
		return EmailVerificationToken.builder()
			.email(email)
			.tokenValue(tokenValue)
			.verified(false)
			.expiresAt(LocalDateTime.now().plus(ttl))
			.build();
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}

	public boolean tokenValueNotMatch(String tokenValue) {
		return !Objects.equals(this.tokenValue, tokenValue);
	}

	public void markVerified() {
		this.verified = true;
	}
}
