package com.tissue.workspace.application.service;

import com.tissue.position.application.service.PositionFinder;
import com.tissue.position.domain.Position;
import com.tissue.team.application.service.TeamFinder;
import com.tissue.team.domain.Team;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.application.service.publisher.WorkspaceEventPublisher;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceMemberManageService implements WorkspaceMemberManageUseCase {

    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final PositionFinder positionFinder;
    private final TeamFinder teamFinder;
    private final WorkspaceAuthorizationService workspaceAuthService;
    private final WorkspaceEventPublisher eventPublisher;

    @Override
    public void updateDisplayName(Long targetMemberId, String displayName, WorkspaceMemberContext actorContext) {

        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, targetMemberId);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(workspace, targetMemberId);
        workspaceMember.updateDisplayName(displayName);
    }

    @Override
    public void updateRole(Long targetMemberId, WorkspaceRole grantRole, WorkspaceMemberContext actorContext) {
        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());
        // TODO: getWithWorkspaceBy
        WorkspaceMember target = workspaceMemberFinder.getBy(workspace, targetMemberId);

        workspaceAuthService.requireRoleGrantPermission(actorContext, grantRole, target.getRole());

        WorkspaceRole oldRole = target.getRole();
        target.updateRole(grantRole);

        eventPublisher.publishWorkspaceRoleChanged(
                target, oldRole, grantRole, actorContext.memberId(), actorContext.displayName());
    }

    @Override
    public void addPosition(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, targetMemberId);

        Position position = positionFinder.getWithWorkspaceBy(actorContext.workspaceKey(), positionId);
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(position.getWorkspace(), targetMemberId);

        workspaceMember.addPosition(position);
    }

    @Override
    public void removePosition(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, targetMemberId);

        Position position = positionFinder.getWithWorkspaceBy(actorContext.workspaceKey(), positionId);
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(position.getWorkspace(), targetMemberId);

        workspaceMember.removePosition(position);
    }

    @Override
    public void addTeam(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, targetMemberId);

        Team team = teamFinder.getWithWorkspaceBy(actorContext.workspaceKey(), teamId);
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(team.getWorkspace(), targetMemberId);

        workspaceMember.addTeam(team);
    }

    @Override
    public void removeTeam(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, targetMemberId);

        Team team = teamFinder.getWithWorkspaceBy(actorContext.workspaceKey(), teamId);
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(team.getWorkspace(), targetMemberId);

        workspaceMember.removeTeam(team);
    }
}
