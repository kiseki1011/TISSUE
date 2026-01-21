package com.tissue.member.adapter.out.persistence;

import com.tissue.member.application.port.out.EmailVerificationJpaRepository;
import com.tissue.member.application.port.out.EmailVerificationRepository;
import com.tissue.member.domain.EmailVerificationToken;
import com.tissue.member.domain.exception.DuplicateVerificationTokenException;
import com.tissue.security.authentication.domain.exception.AuthenticationErrorCode;
import com.tissue.security.authentication.domain.exception.InvalidTokenException;
import java.time.Duration;
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
    public void saveToken(String email, String tokenValue, Duration ttl) {
        EmailVerificationToken verificationToken = tokenRepository
                .findByEmail(email)
                .map(t -> {
                    t.markVerified(); // invalidate token
                    return EmailVerificationToken.create(email, tokenValue, ttl);
                })
                .orElse(EmailVerificationToken.create(email, tokenValue, ttl));
        try {
            tokenRepository.save(verificationToken);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate verification token for email: {}", email, e);
            throw new DuplicateVerificationTokenException(email, e);
        }
    }

    @Override
    @Transactional
    public boolean verify(String email, String tokenValue) {
        EmailVerificationToken token = tokenRepository
                .findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(
                        AuthenticationErrorCode.INVALID_VERIFICATION_TOKEN.getDefaultMessage()));
        if (token.isExpired() || token.tokenValueNotMatch(tokenValue)) {
            return false;
        }

        token.markVerified();
        return true;
    }

    @Override
    public boolean isVerified(String email) {
        return tokenRepository
                .findByEmail(email)
                .map(t -> t.isVerified() && !t.isExpired())
                .orElse(false);
    }

    @Override
    public boolean checkVerifiedToken(String email, String token) {
        return tokenRepository
                .findByEmail(email)
                .map(t -> t.isVerified() && !t.isExpired() && !t.tokenValueNotMatch(token))
                .orElse(false);
    }

    @Override
    @Transactional
    public void deleteToken(String email) {
        tokenRepository.deleteByEmail(email);
    }
}
