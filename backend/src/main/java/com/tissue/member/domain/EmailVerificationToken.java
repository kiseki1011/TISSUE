package com.tissue.member.domain;

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
import lombok.NonNull;

@Entity
@Getter
@Table(name = "email_verification_token", uniqueConstraints = {
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
	private LocalDateTime expiresAt;

	@Builder
	private EmailVerificationToken(String email, String tokenValue, boolean verified, LocalDateTime expiresAt) {
		this.email = email;
		this.tokenValue = tokenValue;
		this.verified = verified;
		this.expiresAt = expiresAt;
	}

	public static EmailVerificationToken create(@NonNull String email, @NonNull String tokenValue,
		@NonNull Duration ttl) {
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
