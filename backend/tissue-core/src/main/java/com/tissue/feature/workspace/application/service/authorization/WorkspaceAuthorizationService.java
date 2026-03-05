package com.tissue.feature.workspace.application.service.authorization;

import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.CANNOT_CHANGE_ROLE_TO_OWNER;
import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.INVITE_LINK_EDIT_NOT_ALLOWED;
import static com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode.ROLE_GRANT_NOT_ALLOWED;

import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.InsufficientWorkspaceRoleException;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceAuthorizationService {

    public void requireWorkspaceAdmin(WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(WorkspaceRole.ADMIN);
    }

    public void requireWorkspaceOwner(WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.OWNER)) {
            return;
        }
        throw new InsufficientWorkspaceRoleException(WorkspaceRole.OWNER);
    }

    public void requireRoleGrantPermission(WorkspaceMember actor, WorkspaceRole grantRole, WorkspaceRole targetRole) {
        if (grantRole == WorkspaceRole.OWNER) {
            throw new ForbiddenException(CANNOT_CHANGE_ROLE_TO_OWNER);
        }
        if (isLowerThanWorkspaceAdmin(actor)) {
            throw new InsufficientWorkspaceRoleException(WorkspaceRole.ADMIN);
        }
        if (actor.getRole().isHigherThan(targetRole)) {
            return;
        }
        throw new ForbiddenException(ROLE_GRANT_NOT_ALLOWED);
    }

    private boolean isLowerThanWorkspaceAdmin(WorkspaceMember actor) {
        return !actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN);
    }

    public void requireInviteLinkEditPermission(WorkspaceInviteLink inviteLink, WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isLinkCreator(inviteLink, actor.getMember().getId())) {
            return;
        }
        throw new ForbiddenException(INVITE_LINK_EDIT_NOT_ALLOWED);
    }

    private boolean isLinkCreator(WorkspaceInviteLink inviteLink, Long actorMemberId) {
        return inviteLink.getCreatedBy().equals(actorMemberId);
    }
}
