package com.tissue.security.application.service;

import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_EMAIL;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_USERNAME;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import com.tissue.shared.exception.base.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAccountValidator {

    private final MemberQueryRepository memberRepository;

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

    /**
     * The last remaining {@code SUPER_ADMIN} cannot withdraw, otherwise the instance would be left
     * without a system administrator.
     */
    public void ensureWithdrawable(Member member) {
        if (member.isSuperAdmin()
                && memberRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE) <= 1) {
            throw new LastSuperAdminException();
        }
    }
}
