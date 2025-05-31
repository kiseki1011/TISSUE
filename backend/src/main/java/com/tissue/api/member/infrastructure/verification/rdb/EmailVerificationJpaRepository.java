package com.tissue.api.member.infrastructure.verification.rdb;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationJpaRepository extends JpaRepository<EmailVerificationToken, Long> {
	Optional<EmailVerificationToken> findByEmail(String email);

	@Modifying
	@Query("DELETE FROM EmailVerificationToken t WHERE t.email = :email")
	void deleteByEmail(@Param("email") String email);
}
