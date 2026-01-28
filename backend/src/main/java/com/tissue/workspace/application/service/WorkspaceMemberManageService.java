package com.tissue.workspace.application.service;

import com.tissue.position.application.service.PositionFinder;
import com.tissue.position.domain.Position;
import com.tissue.team.application.service.TeamFinder;
import com.tissue.team.domain.Team;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.ManagePositionCommand;
import com.tissue.workspace.application.dto.request.ManageTeamCommand;
import com.tissue.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.request.UpdateRoleCommand;
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
    public void updateDisplayName(UpdateDisplayNameCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, cmd.targetMemberId());

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        WorkspaceMember workspaceMember =
                workspaceMemberFinder.getIncludingSoftDeleted(cmd.targetMemberId(), workspace);
        workspaceMember.updateDisplayName(cmd.displayName());
    }

    @Override
    public void updateRole(UpdateRoleCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        WorkspaceMember target = workspaceMemberFinder.getIncludingSoftDeleted(cmd.targetMemberId(), workspace);

        workspaceAuthService.requireRoleGrantPermission(actorContext, cmd.grantRole(), target.getRole());

        WorkspaceRole oldRole = target.getRole();
        target.updateRole(cmd.grantRole());

        eventPublisher.publishWorkspaceRoleChanged(
                target, oldRole, cmd.grantRole(), actorContext.memberId(), actorContext.displayName());
    }

    @Override
    public void addPosition(ManagePositionCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, cmd.targetMemberId());

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Position position = positionFinder.getBy(cmd.positionId(), workspace);
        WorkspaceMember workspaceMember =
                workspaceMemberFinder.getIncludingSoftDeleted(cmd.targetMemberId(), workspace);

        workspaceMember.addPosition(position);

        // TODO: WorkspaceMemberPositionChangedEvent
    }

    @Override
    public void removePosition(ManagePositionCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, cmd.targetMemberId());

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Position position = positionFinder.getBy(cmd.positionId(), workspace);
        WorkspaceMember workspaceMember =
                workspaceMemberFinder.getIncludingSoftDeleted(cmd.targetMemberId(), workspace);

        workspaceMember.removePosition(position);

        // TODO: WorkspaceMemberPositionChangedEvent
    }

    @Override
    public void addTeam(ManageTeamCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, cmd.targetMemberId());

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Team team = teamFinder.getBy(cmd.teamId(), workspace);
        WorkspaceMember workspaceMember =
                workspaceMemberFinder.getIncludingSoftDeleted(cmd.targetMemberId(), workspace);

        workspaceMember.addTeam(team);

        // TODO: WorkspaceMemberTeamChangedEvent
    }

    @Override
    public void removeTeam(ManageTeamCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdminOrSelf(actorContext, cmd.targetMemberId());

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Team team = teamFinder.getBy(cmd.teamId(), workspace);
        WorkspaceMember workspaceMember =
                workspaceMemberFinder.getIncludingSoftDeleted(cmd.targetMemberId(), workspace);

        workspaceMember.removeTeam(team);

        // TODO: WorkspaceMemberTeamChangedEvent
    }
}
