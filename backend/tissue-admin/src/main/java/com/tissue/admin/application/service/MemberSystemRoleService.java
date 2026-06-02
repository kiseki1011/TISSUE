package com.tissue.admin.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SuperAdminGuard;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.CannotDemoteSelfSuperAdminException;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberSystemRoleService {

    private final MemberFinder memberFinder;
    private final SuperAdminGuard superAdminGuard;

    @Transactional
    public void changeSystemRole(Long actorMemberId, Long targetMemberId, SystemRole newRole) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        if (!actor.hasAtLeast(SystemRole.SUPER_ADMIN)) {
            throw new ForbiddenException(MemberErrorCode.SUPER_ADMIN_REQUIRED);
        }

        Member target = memberFinder.getActiveById(targetMemberId);

        if (isDemotionFromSuperAdmin(target, newRole)) {
            ensureNotSelfDemotion(actorMemberId, targetMemberId);
            superAdminGuard.ensureNotLastActiveSuperAdmin(target);
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
}
