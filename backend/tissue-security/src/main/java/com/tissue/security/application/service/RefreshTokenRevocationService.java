package com.tissue.security.application.service;

import com.tissue.security.application.port.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes a member's refresh token in its own transaction.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId);
    }
}
