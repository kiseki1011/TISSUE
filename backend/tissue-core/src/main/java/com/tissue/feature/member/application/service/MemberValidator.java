package com.tissue.feature.member.application.service;

import com.tissue.feature.member.application.port.out.MemberQueryRepository;
import com.tissue.feature.member.config.MemberProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.exception.DuplicateEmailException;
import com.tissue.feature.member.domain.exception.DuplicateUsernameException;
import com.tissue.feature.member.domain.exception.OwnerNotWithdrawableException;
import com.tissue.feature.member.domain.exception.SignupDisabledException;
import com.tissue.feature.member.domain.exception.UnauthorizedDomainException;
import com.tissue.feature.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.support.system.Mode;
import com.tissue.support.system.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;
    private final MemberProperties memberProperties;
    private final SystemProperties systemProperties;

    // TODO: 그냥 username 변경도 MemberAccountService에서 처리할까?
    //  (그게 깔끔하긴 함. 그렇게 하면 아예 MemberValidator를 tissue-security로 옮기는게 가능)
    public void ensureUniqueUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }
    }

    // TODO: move to tissue-security/MemberAccountValidator or just inside MemberAccountService
    public void ensureUniqueEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    // TODO: move to tissue-security/MemberAccountValidator or just inside MemberAccountService
    public void ensureWithdrawable(Member member) {
        boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberAndRole(member, WorkspaceRole.OWNER);
        if (hasOwnedWorkspaces) {
            throw new OwnerNotWithdrawableException(member);
        }
    }

    // TODO: move to tissue-security/MemberSignupValidator or just inside MemberSignupService
    public void ensureSignupAllowed() {
        if (!memberProperties.isAllowSignup()) {
            throw new SignupDisabledException();
        }
    }

    // TODO: move to tissue-security/MemberSignupValidator or just inside MemberSignupService
    public void ensureDomainAllowedIfPrivate(String email) {
        if (systemProperties.getMode() == Mode.PRIVATE) {
            ensureAllowedDomain(email);
        }
    }

    // TODO: move to tissue-security/MemberSignupValidator
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
