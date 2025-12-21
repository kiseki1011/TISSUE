package com.tissue.team.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.common.enums.ColorType;
import com.tissue.team.domain.model.Team;
import com.tissue.team.infrastructure.repository.TeamRepository;
import com.tissue.team.presentation.dto.request.CreateTeamRequest;
import com.tissue.team.presentation.dto.request.UpdateTeamColorRequest;
import com.tissue.team.presentation.dto.request.UpdateTeamRequest;
import com.tissue.team.presentation.dto.response.TeamResponse;
import com.tissue.team.validator.TeamValidator;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCommandService {

	private final TeamFinder teamFinder;
	private final WorkspaceFinder workspaceFinder;
	private final TeamRepository teamRepository;
	private final TeamValidator teamValidator;

	// TODO: refactor so the user can set the color
	@Transactional
	public TeamResponse createTeam(
		String workspaceCode,
		CreateTeamRequest request
	) {
		Workspace workspace = workspaceFinder.getModifiableBy(workspaceCode);

		Team team = Team.builder()
			.name(request.name())
			.description(request.description())
			.color(ColorType.getRandomColor())
			.workspace(workspace)
			.build();

		return TeamResponse.from(teamRepository.save(team));
	}

	// TODO: refactor so the user can set the color
	// TODO: refactor to use Patchers.apply
	@Transactional
	public TeamResponse updateTeam(
		String workspaceCode,
		Long teamId,
		UpdateTeamRequest request
	) {
		Team team = teamFinder.findByIdAndWorkspaceKey(teamId, workspaceCode);

		team.updateName(request.name());
		team.updateDescription(request.description());

		return TeamResponse.from(team);
	}

	// TODO: remove after refactoring createTeam()
	@Transactional
	public TeamResponse updateTeamColor(
		String workspaceCode,
		Long teamId,
		UpdateTeamColorRequest request
	) {
		Team team = teamFinder.findByIdAndWorkspaceKey(teamId, workspaceCode);

		team.updateColor(request.colorType());

		return TeamResponse.from(team);
	}

	@Transactional
	public void deleteTeam(
		String workspaceCode,
		Long teamId
	) {
		Team team = teamFinder.findByIdAndWorkspaceKey(teamId, workspaceCode);

		teamValidator.ensureDeletable(team);

		teamRepository.delete(team);
	}
}
