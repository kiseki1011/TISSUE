package com.tissue.adapter.persistence;

import com.tissue.application.port.repository.EmailVerificationJpaRepository;
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

    private final EmailVerificationJpaRepository tokenRepository;

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final String STATUS_UNKNOWN = "UNKNOWN";

    @Override
    @Transactional
    public String startVerification(String email, String emailToken, Duration ttl) {
        String verificationId = UUID.randomUUID().toString();

        tokenRepository.deleteByEmail(email);

        EmailVerificationToken token = EmailVerificationToken.create(email, emailToken, ttl, verificationId);
        try {
            tokenRepository.save(token);
            return verificationId;
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate verification token for email: {}", email, e);
            throw new DuplicateVerificationTokenException(email, e);
        }
    }

    @Override
    @Transactional
    public boolean verifyByToken(String emailToken, Duration signupTokenTtl) {
        return tokenRepository
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
        return tokenRepository
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
        return tokenRepository
                .findBySignupToken(signupToken)
                .filter(t -> Objects.equals(t.getEmail(), email))
                .map(t -> {
                    tokenRepository.deleteByEmail(email);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteVerification(String verificationId) {
        tokenRepository
                .findByVerificationId(verificationId)
                .ifPresent(t -> tokenRepository.deleteByEmail(t.getEmail()));
    }
}
