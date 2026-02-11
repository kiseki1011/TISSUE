package com.tissue.application.service;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.config.MemberProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.exception.DuplicateEmailException;
import com.tissue.feature.member.domain.exception.DuplicateUsernameException;
import com.tissue.feature.member.domain.exception.OwnerNotWithdrawableException;
import com.tissue.feature.member.domain.exception.SignupDisabledException;
import com.tissue.feature.member.domain.exception.UnauthorizedDomainException;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
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
            throw new DuplicateUsernameException(username);
        }
    }

    public void ensureUniqueEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    public void ensureWithdrawable(Member member) {
        boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberAndRole(member, WorkspaceRole.OWNER);
        if (hasOwnedWorkspaces) {
            throw new OwnerNotWithdrawableException(member);
        }
    }

    public void ensureSignupAllowed() {
        if (!memberProperties.isAllowSignup()) {
            throw new SignupDisabledException();
        }
    }

    public void ensureDomainAllowedIfPrivate(String email) {
        if (systemProperties.getMode() == Mode.PRIVATE) {
            ensureAllowedDomain(email);
        }
    }

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
