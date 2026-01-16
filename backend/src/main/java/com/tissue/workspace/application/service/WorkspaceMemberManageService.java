package com.tissue.workspace.application.service;

import com.tissue.position.application.service.finder.PositionFinder;
import com.tissue.position.domain.Position;
import com.tissue.team.application.service.finder.TeamFinder;
import com.tissue.team.domain.Team;
import com.tissue.workspace.application.dto.in.ManagePositionCommand;
import com.tissue.workspace.application.dto.in.ManageTeamCommand;
import com.tissue.workspace.application.dto.in.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.in.UpdateRoleCommand;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import com.tissue.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.event.WorkspaceEventPublisher;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
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
    //    private final CurrentMemberProvider currentMemberProvider;
    private final WorkspaceEventPublisher eventPublisher;

    @Override
    public void updateDisplayName(UpdateDisplayNameCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        workspaceAuthService.requireWorkspaceAdminOrSelf(cmd.workspaceKey(), cmd.targetMemberId(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        // TODO: should i pass workspace instead of workspaceKey?
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(cmd.targetMemberId(), cmd.workspaceKey());
        workspaceMember.updateDisplayName(cmd.displayName());
    }

    @Override
    public void updateRole(UpdateRoleCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        // TODO: should i pass workspace instead of workspaceKey?
        WorkspaceMember target = workspaceMemberFinder.getBy(cmd.targetMemberId(), cmd.workspaceKey());

        workspaceAuthService.requireRoleGrantPermission(
                cmd.workspaceKey(), cmd.role(), actor.memberId(), target.getRole(), actor.role());

        WorkspaceRole oldRole = target.getRole();
        target.changeRoleTo(cmd.role());

        eventPublisher.publishWorkspaceRoleChanged(target, oldRole, cmd.role(), actor.memberId(), actor.displayName());
    }

    @Override
    public void addPosition(ManagePositionCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        workspaceAuthService.requireWorkspaceAdminOrSelf(cmd.workspaceKey(), cmd.targetMemberId(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Position position = positionFinder.getBy(cmd.positionId(), workspace);
        // TODO: pass workspace instead of workspaceKey
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(cmd.targetMemberId(), cmd.workspaceKey());

        workspaceMember.addPosition(position);

        // TODO: WorkspaceMemberPositionChangedEvent
    }

    @Override
    public void removePosition(ManagePositionCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        workspaceAuthService.requireWorkspaceAdminOrSelf(cmd.workspaceKey(), cmd.targetMemberId(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Position position = positionFinder.getBy(cmd.positionId(), workspace);
        // TODO: pass workspace instead of workspaceKey
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(cmd.targetMemberId(), cmd.workspaceKey());

        workspaceMember.removePosition(position);

        // TODO: WorkspaceMemberPositionChangedEvent
    }

    @Override
    public void addTeam(ManageTeamCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        workspaceAuthService.requireWorkspaceAdminOrSelf(cmd.workspaceKey(), cmd.targetMemberId(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Team team = teamFinder.getBy(cmd.teamId(), cmd.workspaceKey());
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(cmd.targetMemberId(), cmd.workspaceKey());

        workspaceMember.addTeam(team);

        // TODO: WorkspaceMemberTeamChangedEvent
    }

    @Override
    public void removeTeam(ManageTeamCommand cmd) {
        WorkspaceMemberInfo actor = cmd.actor();
        workspaceAuthService.requireWorkspaceAdminOrSelf(cmd.workspaceKey(), cmd.targetMemberId(), actor.memberId());

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Team team = teamFinder.getBy(cmd.teamId(), cmd.workspaceKey());
        WorkspaceMember workspaceMember = workspaceMemberFinder.getBy(cmd.targetMemberId(), cmd.workspaceKey());

        workspaceMember.removeTeam(team);

        // TODO: WorkspaceMemberTeamChangedEvent
    }
}
