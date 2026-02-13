package com.tissue.application.service;

import static com.tissue.domain.exception.AuthenticationErrorCode.EMAIL_SIGNUP_DISABLED;
import static com.tissue.domain.exception.AuthenticationErrorCode.OWNER_NOT_WITHDRAWABLE;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_EMAIL;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_USERNAME;

import com.tissue.domain.exception.UnauthorizedDomainException;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.config.MemberProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.system.Mode;
import com.tissue.support.system.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAccountValidator {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;
    private final MemberProperties memberProperties;
    private final SystemProperties systemProperties;

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
        boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberAndRole(member, WorkspaceRole.OWNER);
        if (hasOwnedWorkspaces) {
            throw new BadRequestException(OWNER_NOT_WITHDRAWABLE);
        }
    }

    public void ensureSignupAllowed() {
        if (!memberProperties.isAllowSignup()) {
            throw new ForbiddenException(EMAIL_SIGNUP_DISABLED);
        }
    }

    public void ensureDomainAllowedIfPrivate(String email) {
        if (systemProperties.getMode() == Mode.PRIVATE) {
            ensureAllowedDomain(email);
        }
    }

    // TODO: refactor
    public void ensureAllowedDomain(String email) {
        if (memberProperties.getAllowedDomains().isEmpty()
                || memberProperties.getAllowedDomains().contains("*")) {
            return;
        }

        String domain = email.substring(email.indexOf("@") + 1);
        if (!memberProperties.getAllowedDomains().contains(domain)) {
            throw new UnauthorizedDomainException(email);
        }
    }
}
