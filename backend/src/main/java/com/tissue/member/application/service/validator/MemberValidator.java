package com.tissue.member.application.service.validator;

import com.tissue.member.adapter.in.web.config.MemberProperties;
import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.DuplicateEmailException;
import com.tissue.member.domain.exception.DuplicateUsernameException;
import com.tissue.member.domain.exception.OwnerNotWithdrawableException;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;
    private final MemberProperties memberProperties;

    // TODO: should i exclude PENDING members?
    //  PENDING members are not members yet.
    //  i think Unique check should be done for ACTIVE, DELETED(just in case of restore)
    public void ensureUniqueEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }

    public void ensureUniqueUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }
    }

    public void ensureWithdrawable(Member member) {
        boolean hasOwnedWorkspaces = workspaceMemberRepository.existsByMemberAndRole(member, WorkspaceRole.OWNER);
        if (hasOwnedWorkspaces) {
            throw new OwnerNotWithdrawableException(member);
        }
    }

    public void ensureAllowedDomain(String email) {
        if (memberProperties.getAllowedDomains().isEmpty() || memberProperties.getAllowedDomains().contains("*")) {
            return;
        }

        String domain = email.substring(email.indexOf("@") + 1);
        // TODO: 커스텀 예외를 만드는게 좋을까?
        if (!memberProperties.getAllowedDomains().contains(domain)) {
            throw new IllegalArgumentException("Email domain not allowed: " + domain);
        }
    }
}
