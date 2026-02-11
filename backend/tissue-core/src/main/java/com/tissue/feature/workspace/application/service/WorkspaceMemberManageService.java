package com.tissue.feature.workspace.application.service;

import com.tissue.feature.organization.position.application.service.PositionFinder;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.team.application.service.TeamFinder;
import com.tissue.feature.organization.team.domain.Team;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberManageUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.application.service.publisher.WorkspaceEventPublisher;
import com.tissue.feature.workspace.domain.Workspace;
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
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
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

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberSearchResponse> searchMembers(
            WorkspaceMemberContext context, String query, @Nullable String projectKey) {

        workspaceAuthorizationService.requireWorkspaceMember(context);

        List<WorkspaceMember> members;

        if (projectKey != null) {
            members = workspaceMemberQueryRepository.searchProjectMembers(context.workspaceKey(), projectKey, query);
        } else {
            members = workspaceMemberQueryRepository.searchMembers(context.workspaceKey(), query);
        }

        return members.stream().map(WorkspaceMemberSearchResponse::from).toList();
    }
}
