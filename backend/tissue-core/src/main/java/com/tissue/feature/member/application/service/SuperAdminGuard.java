package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuperAdminGuard {

    private final MemberQueryRepository memberQueryRepository;

    public void ensureNotLastActiveSuperAdmin(Member member) {
        if (member.isSuperAdmin()
                && memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE) <= 1) {
            throw new LastSuperAdminException();
        }
    }
}
