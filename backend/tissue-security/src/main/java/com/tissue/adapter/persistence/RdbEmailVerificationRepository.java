package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.EmailVerificationRepository;
import com.tissue.domain.EmailVerificationToken;
import com.tissue.domain.exception.DuplicateVerificationTokenException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.email.verification.token-store", havingValue = "rdb", matchIfMissing = true)
@RequiredArgsConstructor
public class RdbEmailVerificationRepository implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository verificationRepository;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_UNKNOWN = "UNKNOWN";

    @Override
    @Transactional
    public void storeVerificationContext(String verificationId, String email, String emailToken, Duration ttl) {
        verificationRepository.deleteByEmail(email);

        EmailVerificationToken token = EmailVerificationToken.create(email, emailToken, ttl, verificationId);
        try {
            verificationRepository.save(token);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate verification token for email: {}", email, e);
            throw new DuplicateVerificationTokenException(email, e);
        }
    }

    @Override
    @Transactional
    public boolean verifyByEmailToken(String emailToken, Duration signupTokenTtl) {
        return verificationRepository
                .findByTokenValue(emailToken)
                .filter(t -> !t.isExpired())
                .map(t -> {
                    String signupToken = UUID.randomUUID().toString();
                    t.markVerified(signupToken, signupTokenTtl);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public VerificationStatus getStatus(String verificationId) {
        return verificationRepository
                .findByVerificationId(verificationId)
                .map(t -> {
                    if (t.isVerified()) {
                        return new VerificationStatus(STATUS_VERIFIED, t.getSignupToken());
                    }
                    return new VerificationStatus(STATUS_PENDING, null);
                })
                .orElse(new VerificationStatus(STATUS_UNKNOWN, null));
    }

    @Override
    @Transactional
    public boolean validateSignupToken(String email, String signupToken) {
        return verificationRepository
                .findBySignupToken(signupToken)
                .filter(t -> Objects.equals(t.getEmail(), email))
                .map(t -> {
                    verificationRepository.deleteByEmail(email);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteVerification(String verificationId) {
        verificationRepository
                .findByVerificationId(verificationId)
                .ifPresent(t -> verificationRepository.deleteByEmail(t.getEmail()));
    }

    @Override
    public void storeResetCode(String email, String code, Duration ttl) {
        // Not implemented for RDB store for now
        log.warn("storeResetCode not implemented for RDB store");
    }

    @Override
    @org.jspecify.annotations.Nullable
    public String verifyResetCode(String email, String code, Duration resetTokenTtl) {
        log.warn("verifyResetCode not implemented for RDB store");
        return null;
    }

    @Override
    @org.jspecify.annotations.Nullable
    public String validateResetToken(String resetToken) {
        log.warn("validateResetToken not implemented for RDB store");
        return null;
    }
}
