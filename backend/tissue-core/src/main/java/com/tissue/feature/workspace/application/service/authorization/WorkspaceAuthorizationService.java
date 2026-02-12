package com.tissue.feature.workspace.application.service.authorization;

import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.CannotChangeRoleToOwnerException;
import com.tissue.feature.workspace.domain.exception.InsufficientWorkspaceRoleException;
import com.tissue.feature.workspace.domain.exception.InviteLinkEditNotAllowedException;
import com.tissue.feature.workspace.domain.exception.WorkspaceAdminOrSelfRequiredException;
import com.tissue.feature.workspace.domain.exception.WorkspaceRoleGrantNotAllowedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceAuthorizationService {

    // New style
    public void requireWorkspaceMember(WorkspaceMember actor) {
        // WorkspaceMember exists means they are a member
    }

    // Compatibility style
    public void requireWorkspaceMember(WorkspaceMemberContext actor) {
        if (actor.isWorkspaceMember()) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.MEMBER);
    }

    // New style
    public void requireWorkspaceAdmin(WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.getWorkspaceKey(), WorkspaceRole.ADMIN);
    }

    // Compatibility style
    public void requireWorkspaceAdmin(WorkspaceMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.ADMIN);
    }

    // New style
    public void requireWorkspaceOwner(WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.OWNER)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.getWorkspaceKey(), WorkspaceRole.OWNER);
    }

    // Compatibility style
    public void requireWorkspaceOwner(WorkspaceMemberContext actor) {
        if (actor.isWorkspaceOwner()) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(actor.workspaceKey(), WorkspaceRole.OWNER);
    }

    // New style
    public void requireWorkspaceAdminOrSelf(WorkspaceMember actor, Long targetMemberId) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (targetMemberId.equals(actor.getMember().getId())) {
            return;
        }
        throw new WorkspaceAdminOrSelfRequiredException(actor.getWorkspaceKey(), targetMemberId);
    }

    // Compatibility style
    public void requireWorkspaceAdminOrSelf(WorkspaceMemberContext actor, Long targetMemberId) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (targetMemberId.equals(actor.memberId())) {
            return;
        }
        throw new WorkspaceAdminOrSelfRequiredException(actor.workspaceKey(), targetMemberId);
    }

    // New style
    public void requireRoleGrantPermission(WorkspaceMember actor, WorkspaceRole grantRole, WorkspaceRole targetRole) {
        if (grantRole == WorkspaceRole.OWNER) {
            throw new CannotChangeRoleToOwnerException();
        }
        if (!actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            throw new InsufficientWorkspaceRoleException(actor.getWorkspaceKey(), WorkspaceRole.ADMIN);
        }
        if (actor.getRole().isHigherThan(targetRole)) {
            return;
        }
        throw new WorkspaceRoleGrantNotAllowedException(actor.getWorkspaceKey(), grantRole);
    }

    // Compatibility style
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

    // New style
    public void requireInviteLinkEditPermission(WorkspaceInviteLink inviteLink, WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isLinkCreator(inviteLink, actor.getMember().getId())) {
            return;
        }
        throw new InviteLinkEditNotAllowedException(inviteLink.getWorkspaceKey(), inviteLink.getId());
    }

    // Compatibility style
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
