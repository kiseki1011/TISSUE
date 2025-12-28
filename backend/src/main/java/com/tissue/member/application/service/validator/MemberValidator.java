package com.tissue.member.application.service.validator;

import com.tissue.member.application.port.out.MemberQueryRepository;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.exception.MemberExceptions;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberValidator {

    private final MemberQueryRepository memberRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;

    // TODO: should i exclude PENDING members?
    //  PENDING members are not members yet.
    //  i think Unique check should be done for ACTIVE, DELETED(just in case of restore)
    public void ensureUniqueEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw MemberExceptions.duplicateEmail(email);
        }
    }

    public void ensureUniqueUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw MemberExceptions.duplicateUsername(username);
        }
    }

    public void ensureWithdrawable(Member member) {
        boolean hasOwnedWorkspaces =
                workspaceMemberRepository.existsByMemberAndRole(member, WorkspaceRole.OWNER);
        if (hasOwnedWorkspaces) {
            throw MemberExceptions.ownerNotWithdrawable(member);
        }
    }
}
