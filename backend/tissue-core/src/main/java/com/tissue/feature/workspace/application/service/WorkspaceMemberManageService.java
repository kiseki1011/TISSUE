package com.tissue.feature.workspace.application.service;

import com.tissue.feature.organization.position.application.service.PositionFinder;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.team.application.service.TeamFinder;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberManageUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.application.service.publisher.WorkspaceEventPublisher;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceMemberManageService implements WorkspaceMemberManageUseCase {

    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final PositionFinder positionFinder;
    private final TeamFinder teamFinder;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceEventPublisher eventPublisher;

    @Override
    public void updateDisplayName(String workspaceKey, String displayName, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        actor.updateDisplayName(displayName);
    }

    @Override
    public void updateRole(String workspaceKey, Long targetMemberId, WorkspaceRole grantRole, Long actorMemberId) {
        WorkspaceMember target = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        workspaceAuthorizationService.requireRoleGrantPermission(actor, grantRole, target.getRole());

        WorkspaceRole oldRole = target.getRole();
        target.updateRole(grantRole);

        eventPublisher.publishWorkspaceRoleChanged(
                target, oldRole, grantRole, actor.getMemberId(), actor.getDisplayName());
    }

    @Override
    public void addPosition(String workspaceKey, Long targetMemberId, Long positionId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Position position = positionFinder.getWithWorkspaceBy(workspaceKey, positionId);
        WorkspaceMember target = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);

        target.addPosition(position);
    }

    @Override
    public void removePosition(String workspaceKey, Long targetMemberId, Long positionId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Position position = positionFinder.getWithWorkspaceBy(workspaceKey, positionId);
        WorkspaceMember target = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);

        target.removePosition(position);
    }

    @Override
    public void addTeam(String workspaceKey, Long targetMemberId, Long teamId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Team team = teamFinder.getWithWorkspaceBy(workspaceKey, teamId);
        WorkspaceMember target = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);

        target.addTeam(team);
    }

    @Override
    public void removeTeam(String workspaceKey, Long targetMemberId, Long teamId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Team team = teamFinder.getWithWorkspaceBy(workspaceKey, teamId);
        WorkspaceMember target = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);

        target.removeTeam(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberSearchResponse> searchMembers(
            String workspaceKey, @Nullable String projectKey, String query, Long actorMemberId) {

        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        List<WorkspaceMember> members;

        if (projectKey != null) {
            members = workspaceMemberQueryRepository.searchProjectMembers(workspaceKey, projectKey, query);
        } else {
            members = workspaceMemberQueryRepository.searchMembers(workspaceKey, query);
        }

        return members.stream().map(WorkspaceMemberSearchResponse::from).toList();
    }
}
