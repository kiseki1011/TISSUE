package com.tissue.team.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import com.tissue.team.application.dto.request.CreateTeamCommand;
import com.tissue.team.application.dto.request.UpdateTeamCommand;
import com.tissue.team.application.dto.response.GetTeams;
import com.tissue.team.application.dto.response.TeamCreateResponse;
import com.tissue.team.application.dto.response.TeamDetail;
import com.tissue.team.application.port.in.TeamUseCase;
import com.tissue.team.application.port.out.TeamCommandRepository;
import com.tissue.team.application.port.out.TeamQueryRepository;
import com.tissue.team.application.service.finder.TeamFinder;
import com.tissue.team.application.service.validator.TeamValidator;
import com.tissue.team.domain.Team;
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
    private final CurrentMemberProvider currentMemberProvider;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public TeamCreateResponse create(CreateTeamCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(cmd.workspaceKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        teamValidator.ensureUniqueName(workspace, cmd.name());

        Team team = Team.builder()
                .workspace(workspace)
                .name(cmd.name())
                .description(cmd.description())
                .color(cmd.color())
                .build();

        return TeamCreateResponse.from(teamCommandRepository.save(team));
    }

    @Override
    public void update(UpdateTeamCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(cmd.workspaceKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
        Team team = teamFinder.getBy(cmd.teamId(), workspace);

        Patchers.apply(cmd.name(), newName -> {
            if (team.getName().isSameAs(newName)) {
                return;
            }
            teamValidator.ensureUniqueName(workspace, newName);
            team.updateName(newName);
        });
        Patchers.apply(cmd.description(), team::updateDescription);
        Patchers.apply(cmd.color(), team::updateColor);
    }

    @Override
    public void delete(String workspaceKey, Long teamId) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(workspaceKey, actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(workspaceKey);
        Team team = teamFinder.getBy(teamId, workspace);

        teamValidator.ensureDeletable(team);

        teamCommandRepository.delete(team);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDetail getTeam(String workspaceKey, Long teamId) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceMember(workspaceKey, actorMemberId);

        Team team = teamFinder.getBy(teamId, workspaceKey);
        return TeamDetail.from(team);
    }

    @Override
    @Transactional(readOnly = true)
    public GetTeams getTeams(String workspaceKey) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceMember(workspaceKey, actorMemberId);

        List<Team> teams = teamQueryRepository.findAllByWorkspace_KeyOrderByCreatedAtAsc(workspaceKey);
        return GetTeams.from(teams);
    }
}
