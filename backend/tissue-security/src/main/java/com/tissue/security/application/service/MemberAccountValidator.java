package com.tissue.security.application.service;

import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_EMAIL;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_USERNAME;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.service.SuperAdminGuard;
import com.tissue.feature.member.domain.Member;
import com.tissue.shared.exception.base.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAccountValidator {

    private final MemberQueryRepository memberRepository;
    private final SuperAdminGuard superAdminGuard;

    public void ensureUniqueUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new ResourceConflictException(DUPLICATE_USERNAME);
        }
    }

    public void ensureUniqueEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new ResourceConflictException(DUPLICATE_EMAIL);
        }
    }

    public void ensureWithdrawable(Member member) {
        superAdminGuard.ensureNotLastActiveSuperAdmin(member);
    }
}
