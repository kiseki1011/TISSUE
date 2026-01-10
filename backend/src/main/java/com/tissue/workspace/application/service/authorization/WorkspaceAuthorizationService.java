package com.tissue.workspace.application.service.authorization;

import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.CannotChangeRoleToOwnerException;
import com.tissue.workspace.domain.exception.InsufficientWorkspaceRoleException;
import com.tissue.workspace.domain.exception.InviteLinkEditNotAllowedException;
import com.tissue.workspace.domain.exception.WorkspaceAdminOrSelfRequiredException;
import com.tissue.workspace.domain.exception.WorkspaceRoleGrantNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceAuthorizationService {

    private final WorkspaceLinkQueryRepository linkRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    public void requireWorkspaceMember(String workspaceKey, Long actorMemberId) {
        if (isMember(workspaceKey, actorMemberId)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(workspaceKey, WorkspaceRole.MEMBER);
    }

    public void requireWorkspaceAdmin(String workspaceKey, Long actorMemberId) {
        if (isAdmin(workspaceKey, actorMemberId)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(workspaceKey, WorkspaceRole.ADMIN);
    }

    public void requireWorkspaceOwner(String workspaceKey, Long actorMemberId) {
        if (isOwner(workspaceKey, actorMemberId)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(workspaceKey, WorkspaceRole.OWNER);
    }

    // TODO: should i consider taking in the WorkspaceMember as the parameter instead of memberId?
    public void requireWorkspaceAdminOrSelf(String workspaceKey, Long targetMemberId, Long actorMemberId) {
        if (isAdmin(workspaceKey, actorMemberId) || targetMemberId.equals(actorMemberId)) {
            return;
        }
        throw new WorkspaceAdminOrSelfRequiredException(workspaceKey, targetMemberId);
    }

    public void requireRoleGrantPermission(
            String workspaceKey, WorkspaceRole grantRole, WorkspaceMember target, WorkspaceMember actor) {
        if (grantRole == WorkspaceRole.OWNER) {
            throw new CannotChangeRoleToOwnerException();
        }
        if (!isAdmin(workspaceKey, actor.getMemberId())) {
            throw new InsufficientWorkspaceRoleException(workspaceKey, WorkspaceRole.ADMIN);
        }
        if (hasHigherRoleThan(target, actor)) {
            return;
        }
        throw new WorkspaceRoleGrantNotAllowedException(workspaceKey, grantRole);
    }

    public void requireInviteLinkEditPermission(String workspaceKey, String token, Long actorMemberId) {
        if (isAdmin(workspaceKey, actorMemberId) || isLinkCreator(token, actorMemberId)) {
            return;
        }
        throw new InviteLinkEditNotAllowedException(workspaceKey, token);
    }

    public boolean isMember(String workspaceKey, Long actorMemberId) {
        return hasWorkspaceRole(workspaceKey, actorMemberId, WorkspaceRole.MEMBER);
    }

    public boolean isAdmin(String workspaceKey, Long actorMemberId) {
        return hasWorkspaceRole(workspaceKey, actorMemberId, WorkspaceRole.ADMIN);
    }

    public boolean isOwner(String workspaceKey, Long actorMemberId) {
        return hasWorkspaceRole(workspaceKey, actorMemberId, WorkspaceRole.OWNER);
    }

    // TODO: should i consider taking in the WorkspaceMember as the parameter instead of memberId?
    //  but in some cases, there is no existing WorkspaceMember entity that was fetched in the service
    private boolean hasWorkspaceRole(String workspaceKey, Long actorMemberId, WorkspaceRole requiredRole) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(actorMemberId, workspaceKey)
                .map(member -> member.getRole().isEqualOrHigherThan(requiredRole))
                .orElse(false);
    }

    private boolean hasHigherRoleThan(WorkspaceMember actor, WorkspaceMember target) {
        return actor.getRole().isHigherThan(target.getRole());
    }

    private boolean isLinkCreator(String token, Long actorMemberId) {
        return linkRepository
                .findByToken(token)
                .map(link -> link.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }
}
