package com.tissue.member.adapter.persistence;

import com.tissue.member.application.port.out.EmailVerificationJpaRepository;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import com.tissue.member.domain.EmailVerificationToken;
import com.tissue.member.domain.exception.DuplicateVerificationTokenException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(name = "tissue.email.verification.strategy", havingValue = "rdb", matchIfMissing = true)
@RequiredArgsConstructor
public class RdbEmailVerificationRepository implements EmailVerificationRepository {

    private final EmailVerificationJpaRepository tokenRepository;

    @Override
    @Transactional
    public String startVerification(String email, String emailToken, Duration ttl) {
        String verificationId = UUID.randomUUID().toString();

        // remove existing token
        tokenRepository.deleteByEmail(email);

        EmailVerificationToken token = EmailVerificationToken.create(email, emailToken, ttl,
            verificationId);
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
    public boolean verifyByToken(String emailToken) {
        return tokenRepository
            .findByTokenValue(emailToken)
            .filter(t -> !t.isExpired())
            .map(t -> {
                String signupToken = UUID.randomUUID().toString();
                t.markVerified(signupToken);
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
                    return new VerificationStatus("VERIFIED", t.getSignupToken());
                }
                return new VerificationStatus("PENDING", null);
            })
            .orElse(new VerificationStatus("UNKNOWN", null));
    }

    @Override
    @Transactional
    public boolean validateSignupToken(String email, String signupToken) {
        return tokenRepository
            .findBySignupToken(signupToken)
            .filter(t -> t.getEmail().equals(email))
            .map(t -> {
                tokenRepository.deleteByEmail(email); // Consume token
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
