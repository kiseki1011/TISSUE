package com.tissue.team.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.common.util.Patchers;
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
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService implements TeamUseCase {

	private final TeamFinder teamFinder;
	private final WorkspaceFinder workspaceFinder;
	private final TeamCommandRepository teamCommandRepository;
	private final TeamQueryRepository teamQueryRepository;
	private final TeamValidator teamValidator;

	@Override
	@Transactional
	public TeamCreateResponse create(CreateTeamCommand cmd) {
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
	@Transactional
	public void update(UpdateTeamCommand cmd) {
		Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
		Team team = teamFinder.getBy(cmd.teamId(), workspace);

		Patchers.apply(cmd.name(), newName -> {
			if ((team.getName().isSameAs(newName))) {
				return;
			}
			teamValidator.ensureUniqueName(workspace, newName);
			team.updateName(newName);
		});
		Patchers.apply(cmd.description(), team::updateDescription);
		Patchers.apply(cmd.color(), team::updateColor);
	}

	@Override
	@Transactional
	public void delete(String workspaceKey, Long teamId) {
		Workspace workspace = workspaceFinder.getModifiableBy(workspaceKey);
		Team team = teamFinder.getBy(teamId, workspace);

		teamValidator.ensureDeletable(team);

		teamCommandRepository.delete(team);
	}

	@Override
	@Transactional(readOnly = true)
	public TeamDetail getTeam(String workspaceKey, Long teamId) {
		Team team = teamFinder.getBy(teamId, workspaceKey);
		return TeamDetail.from(team);
	}

	@Override
	@Transactional(readOnly = true)
	public GetTeams getTeams(String workspaceKey) {
		List<Team> teams = teamQueryRepository.findAllByWorkspace_KeyOrderByCreatedAtAsc(workspaceKey);
		return GetTeams.from(teams);
	}
}
