package com.tissue.workspace.application.service.authorization;

import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
        throw new AccessDeniedException("Requires workspace " + WorkspaceRole.MEMBER.name());
    }

    public void requireWorkspaceAdmin(String workspaceKey, Long actorMemberId) {
        if (isAdmin(workspaceKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires workspace " + WorkspaceRole.ADMIN.name());
    }

    public void requireWorkspaceOwner(String workspaceKey, Long actorMemberId) {
        if (isOwner(workspaceKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires workspace " + WorkspaceRole.OWNER.name());
    }

    public void requireWorkspaceAdminOrSelf(String workspaceKey, Long targetMemberId, Long actorMemberId) {
        if (isAdmin(workspaceKey, actorMemberId) || targetMemberId.equals(actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires workspace %s or the modification target must be yourself"
                .formatted(WorkspaceRole.ADMIN.name()));
    }

    public void requireRoleEditPermission(
            String workspaceKey, WorkspaceRole grantRole, Long targetMemberId, Long actorMemberId) {
        if (grantRole == WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Cannot grant workspace OWNER. Use workspace owner transfer instead.");
        }
        if (!isAdmin(workspaceKey, actorMemberId)) {
            throw new AccessDeniedException("Requires workspace " + WorkspaceRole.ADMIN.name());
        }
        if (hasEqualOrHigherRoleThan(workspaceKey, targetMemberId, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires higher workspace role than target");
    }

    public void requireGrantRolePermission(String workspaceKey, WorkspaceRole grantRole, Long actorMemberId) {
        if (grantRole == WorkspaceRole.OWNER) {
            throw new AccessDeniedException("Cannot grant workspace OWNER. Use workspace owner transfer instead.");
        }
        if (isAdmin(workspaceKey, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires workspace " + WorkspaceRole.ADMIN.name());
    }

    public void requireInviteLinkEditPermission(String workspaceKey, String token, Long actorMemberId) {
        if (isAdmin(workspaceKey, actorMemberId) || isLinkCreator(token, actorMemberId)) {
            return;
        }
        throw new AccessDeniedException("Requires workspace %s or link creator".formatted(WorkspaceRole.ADMIN.name()));
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

    private boolean hasWorkspaceRole(String workspaceKey, Long actorMemberId, WorkspaceRole requiredRole) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(actorMemberId, workspaceKey)
                .map(member -> member.getRole().isEqualOrHigherThan(requiredRole))
                .orElse(false);
    }

    private boolean hasEqualOrHigherRoleThan(String workspaceKey, Long targetMemberId, Long actorMemberId) {
        return workspaceMemberQueryRepository
                .findByMember_IdAndWorkspaceKey(targetMemberId, workspaceKey)
                .map(target -> hasWorkspaceRole(workspaceKey, actorMemberId, target.getRole()))
                .orElse(false);
    }

    private boolean isLinkCreator(String token, Long actorMemberId) {
        return linkRepository
                .findByToken(token)
                .map(link -> link.getCreatedBy().equals(actorMemberId))
                .orElse(false);
    }
}
