package com.tissue.api.team.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.infrastructure.repository.TeamRepository;
import com.tissue.api.team.presentation.dto.request.CreateTeamRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamColorRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamRequest;
import com.tissue.api.team.presentation.dto.response.TeamResponse;
import com.tissue.api.team.validator.TeamValidator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.application.service.command.WorkspaceReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCommandService {

	private final TeamReader teamReader;
	private final WorkspaceReader workspaceReader;
	private final TeamRepository teamRepository;
	private final TeamValidator teamValidator;

	@Transactional
	public TeamResponse createTeam(
		String workspaceCode,
		CreateTeamRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		Team team = Team.builder()
			.name(request.name())
			.description(request.description())
			.color(ColorType.getRandomColor())
			.workspace(workspace)
			.build();

		return TeamResponse.from(teamRepository.save(team));
	}

	@Transactional
	public TeamResponse updateTeam(
		String workspaceCode,
		Long teamId,
		UpdateTeamRequest request
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);

		team.updateName(request.name());
		team.updateDescription(request.description());

		return TeamResponse.from(team);
	}

	@Transactional
	public TeamResponse updateTeamColor(
		String workspaceCode,
		Long teamId,
		UpdateTeamColorRequest request
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);

		team.updateColor(request.colorType());

		return TeamResponse.from(team);
	}

	@Transactional
	public void deleteTeam(
		String workspaceCode,
		Long teamId
	) {
		Team team = teamReader.findTeam(teamId, workspaceCode);

		teamValidator.validateTeamIsUsed(team);

		teamRepository.delete(team);
	}
}
