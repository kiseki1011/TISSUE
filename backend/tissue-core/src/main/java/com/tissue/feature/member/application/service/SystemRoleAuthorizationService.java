package com.tissue.feature.member.application.service;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.shared.exception.base.ForbiddenException;
import org.springframework.stereotype.Component;

/**
 * Authorizes actions against a member's instance-wide {@link SystemRole}.
 *
 * <p>Used by globally-managed resources (such as workflows and issue types) where authorization
 * is based on the actor's system role rather than project membership.
 */
@Component
public class SystemRoleAuthorizationService {

    public void requireSystemAdmin(Member actor) {
        if (actor.hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        throw new ForbiddenException(MemberErrorCode.SYSTEM_ADMIN_REQUIRED);
    }
}
