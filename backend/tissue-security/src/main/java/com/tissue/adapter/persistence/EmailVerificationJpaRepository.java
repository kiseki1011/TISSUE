package com.tissue.adapter.persistence;

import com.tissue.domain.EmailVerificationToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationJpaRepository extends Repository<EmailVerificationToken, Long> {

    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByEmail(String email);

    Optional<EmailVerificationToken> findByTokenValue(String tokenValue);

    Optional<EmailVerificationToken> findByVerificationId(String verificationId);

    Optional<EmailVerificationToken> findBySignupToken(String signupToken);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt <= :now")
    void deleteByExpiresAtBefore(@Param("now") Instant now);
}
