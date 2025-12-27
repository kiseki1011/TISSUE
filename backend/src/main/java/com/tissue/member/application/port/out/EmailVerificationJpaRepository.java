package com.tissue.member.application.port.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.member.domain.EmailVerificationToken;

public interface EmailVerificationJpaRepository extends Repository<EmailVerificationToken, Long> {

	EmailVerificationToken save(EmailVerificationToken token);

	Optional<EmailVerificationToken> findByEmail(String email);

	@Modifying
	@Query("DELETE FROM EmailVerificationToken t WHERE t.email = :email")
	void deleteByEmail(@Param("email") String email);
}
