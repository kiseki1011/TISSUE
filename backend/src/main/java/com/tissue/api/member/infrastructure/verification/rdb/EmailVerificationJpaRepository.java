package com.tissue.api.member.infrastructure.verification.rdb;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationToken, Long> {
	Optional<EmailVerificationToken> findByEmail(String email);
}
