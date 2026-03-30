package com.tissue.security.adapter.persistence;

import com.tissue.security.application.port.repository.EmailVerificationRepository;
import com.tissue.security.domain.EmailVerificationToken;
import com.tissue.security.domain.exception.DuplicateVerificationTokenException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.use-redis", havingValue = "false", matchIfMissing = true)
@RequiredArgsConstructor
public class RdbEmailVerificationRepository implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository verificationRepository;

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
    public boolean verifyByEmailToken(String emailToken, Duration verifiedTokenTtl) {
        return verificationRepository
                .findByTokenValue(emailToken)
                .filter(t -> !t.isExpired())
                .map(t -> {
                    String verifiedToken = UUID.randomUUID().toString();
                    t.markVerified(verifiedToken, verifiedTokenTtl);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationStatus getStatus(String verificationId) {
        return verificationRepository
                .findByVerificationId(verificationId)
                .map(t -> {
                    if (t.isVerified()) {
                        return new VerificationStatus(Status.VERIFIED, t.getVerifiedToken());
                    }
                    return new VerificationStatus(Status.PENDING, null);
                })
                .orElse(new VerificationStatus(Status.UNKNOWN, null));
    }

    @Override
    @Transactional
    public @Nullable String validateVerifiedToken(String verifiedToken) {
        return verificationRepository
                .findByVerifiedToken(verifiedToken)
                .filter(t -> !t.isExpired())
                .map(t -> {
                    String email = t.getEmail();
                    verificationRepository.deleteByEmail(email);
                    return email;
                })
                .orElse(null);
    }
}
