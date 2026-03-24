package com.tissue.security.application.service;

import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_EMAIL;
import static com.tissue.feature.member.domain.exception.MemberErrorCode.DUPLICATE_USERNAME;
import static com.tissue.security.domain.exception.AuthenticationErrorCode.EMAIL_SIGNUP_DISABLED;
import static com.tissue.security.domain.exception.AuthenticationErrorCode.OWNER_NOT_WITHDRAWABLE;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.security.config.SignupProperties;
import com.tissue.security.domain.exception.UnauthorizedDomainException;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ForbiddenException;
import com.tissue.shared.exception.base.ResourceConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberAccountValidator {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;
    private final SignupProperties signupProperties;

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
        if (!signupProperties.isEnabled()) {
            throw new ForbiddenException(EMAIL_SIGNUP_DISABLED);
        }
    }

    public void ensureDomainAllowed(String email) {
        if (!signupProperties.isDomainRestricted()) {
            return;
        }

        String domain = extractDomain(email);
        if (!signupProperties.getAllowedDomains().contains(domain)) {
            throw new UnauthorizedDomainException(email);
        }
    }

    private String extractDomain(String email) {
        return email.substring(email.lastIndexOf("@") + 1);
    }
}
