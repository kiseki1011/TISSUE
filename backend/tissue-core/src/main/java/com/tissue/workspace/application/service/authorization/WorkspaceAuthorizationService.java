package com.tissue.workspace.application.service.authorization;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.domain.WorkspaceInviteLink;
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

    public void requireWorkspaceMember(WorkspaceMemberContext actor) {
        if (actor.isWorkspaceMember()) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.MEMBER);
    }

    public void requireWorkspaceAdmin(WorkspaceMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.ADMIN);
    }

    public void requireWorkspaceOwner(WorkspaceMemberContext actor) {
        if (actor.isWorkspaceOwner()) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.OWNER);
    }

    public void requireWorkspaceAdminOrSelf(WorkspaceMemberContext actor, Long targetMemberId) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (targetMemberId.equals(actor.memberId())) {
            return;
        }
        throw new WorkspaceAdminOrSelfRequiredException(actor.workspaceKey(), targetMemberId);
    }

    public void requireRoleGrantPermission(
            WorkspaceMemberContext actor, WorkspaceRole grantRole, WorkspaceRole targetRole) {
        if (grantRole == WorkspaceRole.OWNER) {
            throw new CannotChangeRoleToOwnerException();
        }
        if (!actor.isWorkspaceAdmin()) {
            throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.ADMIN);
        }
        if (actor.workspaceRole().isHigherThan(targetRole)) {
            return;
        }
        throw new WorkspaceRoleGrantNotAllowedException(actor.workspaceKey(), grantRole);
    }

    public void requireInviteLinkEditPermission(WorkspaceInviteLink inviteLink, WorkspaceMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isLinkCreator(inviteLink, actor.memberId())) {
            return;
        }
        throw new InviteLinkEditNotAllowedException(inviteLink.getWorkspaceKey(), inviteLink.getId());
    }

    private boolean isLinkCreator(WorkspaceInviteLink inviteLink, Long actorMemberId) {
        return inviteLink.getCreatedBy().equals(actorMemberId);
    }
}
