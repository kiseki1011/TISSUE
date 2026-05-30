package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.CannotDemoteSelfSuperAdminException;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberSystemRoleService {

    private final MemberFinder memberFinder;
    private final MemberQueryRepository memberQueryRepository;

    @Transactional
    public void changeSystemRole(Long actorMemberId, Long targetMemberId, SystemRole newRole) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        if (!actor.hasAtLeast(SystemRole.ADMIN)) {
            throw new ForbiddenException(MemberErrorCode.SYSTEM_ADMIN_REQUIRED);
        }
        // TODO: (tissue-admin/SystemPermission): full role-change policy (only SUPER_ADMIN may
        //   change a SUPER_ADMIN, ADMIN may not self-promote). This is only a fail-closed baseline.

        Member target = memberFinder.getActiveById(targetMemberId);

        if (isDemotionFromSuperAdmin(target, newRole)) {
            ensureNotSelfDemotion(actorMemberId, targetMemberId);
            ensureNotLastSuperAdmin();
        }

        target.changeRole(newRole);
    }

    private boolean isDemotionFromSuperAdmin(Member target, SystemRole newRole) {
        return target.isSuperAdmin() && newRole != SystemRole.SUPER_ADMIN;
    }

    private void ensureNotSelfDemotion(Long actorMemberId, Long targetMemberId) {
        if (actorMemberId.equals(targetMemberId)) {
            throw new CannotDemoteSelfSuperAdminException(targetMemberId);
        }
    }

    private void ensureNotLastSuperAdmin() {
        long activeSuperAdmins =
                memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE);
        if (activeSuperAdmins <= 1) {
            throw new LastSuperAdminException();
        }
    }
}
