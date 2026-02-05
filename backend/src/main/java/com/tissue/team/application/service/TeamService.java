package com.tissue.team.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.team.application.dto.request.CreateTeamCommand;
import com.tissue.team.application.dto.request.UpdateTeamCommand;
import com.tissue.team.application.dto.response.GetTeams;
import com.tissue.team.application.dto.response.TeamCreateResponse;
import com.tissue.team.application.dto.response.TeamDetail;
import com.tissue.team.application.port.in.TeamUseCase;
import com.tissue.team.application.port.out.TeamCommandRepository;
import com.tissue.team.application.port.out.TeamQueryRepository;
import com.tissue.team.domain.Team;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamService implements TeamUseCase {

    private final TeamFinder teamFinder;
    private final WorkspaceFinder workspaceFinder;
    private final TeamCommandRepository teamCommandRepository;
    private final TeamQueryRepository teamQueryRepository;
    private final TeamValidator teamValidator;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public TeamCreateResponse create(CreateTeamCommand cmd, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());
        teamValidator.ensureUniqueName(workspace, cmd.name());

        Team team = Team.create(workspace, cmd.name(), cmd.description(), cmd.color());

        return TeamCreateResponse.from(teamCommandRepository.save(team));
    }

    @Override
    public void update(Long teamId, UpdateTeamCommand cmd, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Team team = teamFinder.getWithWorkspaceBy(actorContext.workspaceKey(), teamId);

        Patchers.apply(cmd.name(), newName -> {
            if (team.getName().isSameAs(newName)) {
                return;
            }
            teamValidator.ensureUniqueName(team.getWorkspace(), newName);
            team.updateName(newName);
        });
        Patchers.apply(cmd.description(), team::updateDescription);
        Patchers.apply(cmd.color(), team::updateColor);
    }

    @Override
    public void delete(Long teamId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Team team = teamFinder.getWithWorkspaceBy(actorContext.workspaceKey(), teamId);
        team.ensureEditable();

        teamValidator.ensureDeletable(team);

        teamCommandRepository.delete(team);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetail getTeam(Long teamId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceMember(actorContext);
        Team team = teamFinder.getBy(actorContext.workspaceKey(), teamId);
        return TeamDetail.from(team);
    }

    @Override
    @Transactional(readOnly = true)
    public GetTeams getTeams(WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceMember(actorContext);
        List<Team> teams = teamQueryRepository.findAllByWorkspace_KeyOrderByCreatedAtAsc(actorContext.workspaceKey());
        return GetTeams.from(teams);
    }
}
