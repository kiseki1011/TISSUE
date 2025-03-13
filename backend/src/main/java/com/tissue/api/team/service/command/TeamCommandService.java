package com.tissue.api.team.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.repository.TeamRepository;
import com.tissue.api.team.presentation.dto.request.CreateTeamRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamColorRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamRequest;
import com.tissue.api.team.presentation.dto.response.CreateTeamResponse;
import com.tissue.api.team.presentation.dto.response.DeleteTeamResponse;
import com.tissue.api.team.presentation.dto.response.UpdateTeamColorResponse;
import com.tissue.api.team.presentation.dto.response.UpdateTeamResponse;
import com.tissue.api.team.service.query.TeamQueryService;
import com.tissue.api.team.validator.TeamValidator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.query.WorkspaceReader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCommandService {

	private final TeamQueryService teamQueryService;
	private final WorkspaceReader workspaceReader;
	private final TeamRepository teamRepository;
	private final TeamValidator teamValidator;

	@Transactional
	public CreateTeamResponse createTeam(
		String workspaceCode,
		CreateTeamRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		ColorType randomColor = ColorType.getRandomUnusedColor(workspace.getUsedTeamColors());

		Team team = Team.builder()
			.name(request.name())
			.description(request.description())
			.color(randomColor)
			.workspace(workspace)
			.build();

		return CreateTeamResponse.from(teamRepository.save(team));
	}

	@Transactional
	public UpdateTeamResponse updateTeam(
		String workspaceCode,
		Long teamId,
		UpdateTeamRequest request
	) {
		Team team = teamQueryService.findTeam(teamId, workspaceCode);

		team.updateName(request.name());
		team.updateDescription(request.description());

		return UpdateTeamResponse.from(team);
	}

	@Transactional
	public UpdateTeamColorResponse updateTeamColor(
		String workspaceCode,
		Long teamId,
		UpdateTeamColorRequest request
	) {
		Team team = teamQueryService.findTeam(teamId, workspaceCode);

		team.updateColor(request.colorType());

		return UpdateTeamColorResponse.from(team);
	}

	@Transactional
	public DeleteTeamResponse deleteTeam(
		String workspaceCode,
		Long teamId
	) {
		Team team = teamQueryService.findTeam(teamId, workspaceCode);

		teamValidator.validateTeamIsUsed(team);

		teamRepository.delete(team);

		return DeleteTeamResponse.from(team);
	}
}
