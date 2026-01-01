package com.tissue.workspace.application.service.authorization;

import static com.tissue.workspace.domain.enums.WorkspaceRole.ADMIN;
import static com.tissue.workspace.domain.enums.WorkspaceRole.MEMBER;
import static com.tissue.workspace.domain.enums.WorkspaceRole.OWNER;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceAuthorizationService {

    private final WorkspaceLinkQueryRepository linkRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    public boolean isMember(String workspaceKey, MemberUserDetails userDetails) {
        return hasWorkspaceRole(workspaceKey, userDetails, MEMBER);
    }

    public boolean isAdmin(String workspaceKey, MemberUserDetails userDetails) {
        return hasWorkspaceRole(workspaceKey, userDetails, ADMIN);
    }

    public boolean isOwner(String workspaceKey, MemberUserDetails userDetails) {
        return hasWorkspaceRole(workspaceKey, userDetails, OWNER);
    }

    public boolean isSelfModification(String workspaceKey, Long memberId, MemberUserDetails userDetails) {
        return isAdmin(workspaceKey, userDetails) || memberId.equals(userDetails.getMemberId());
    }

    public boolean hasHigherRoleThanTarget(String workspaceKey, Long targetMemberId, MemberUserDetails userDetails) {
        if (targetMemberId.equals(userDetails.getMemberId())) {
            return false;
        }
        if (isNotAdmin(workspaceKey, userDetails)) {
            return false;
        }
        return hasHigherRoleThan(workspaceKey, targetMemberId, userDetails);
    }

    public boolean canGrantRole(String workspaceKey, WorkspaceRole grantRole, MemberUserDetails userDetails) {
        if (grantRole == WorkspaceRole.OWNER) {
            return false;
        }
        if (isNotAdmin(workspaceKey, userDetails)) {
            return false;
        }
        return hasWorkspaceRole(workspaceKey, userDetails, grantRole);
    }

    public boolean canEditInviteLink(String workspaceKey, String token, MemberUserDetails userDetails) {
        return isAdmin(workspaceKey, userDetails) || isLinkCreator(token, userDetails);
    }

    private boolean hasWorkspaceRole(String workspaceKey, MemberUserDetails userDetails, WorkspaceRole requiredRole) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(userDetails.getMemberId(), workspaceKey)
                .map(member -> member.getRole().isEqualOrHigherThan(requiredRole))
                .orElse(false);
    }

    private boolean isLinkCreator(String token, MemberUserDetails userDetails) {
        return linkRepository
                .findByToken(token)
                .map(link -> link.getCreatedBy().equals(userDetails.getMemberId()))
                .orElse(false);
    }

    private boolean hasHigherRoleThan(String workspaceKey, Long targetMemberId, MemberUserDetails userDetails) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(targetMemberId, workspaceKey)
                .map(target -> hasWorkspaceRole(workspaceKey, userDetails, target.getRole()))
                .orElse(false);
    }

    private boolean isNotAdmin(String workspaceKey, MemberUserDetails userDetails) {
        return !isAdmin(workspaceKey, userDetails);
    }
}
