package com.tissue.api.team.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.common.exception.ResourceNotFoundException;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.respository.TeamRepository;
import com.tissue.api.team.presentation.dto.request.CreateTeamRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamColorRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamRequest;
import com.tissue.api.team.presentation.dto.response.CreateTeamResponse;
import com.tissue.api.team.presentation.dto.response.DeleteTeamResponse;
import com.tissue.api.team.presentation.dto.response.UpdateTeamColorResponse;
import com.tissue.api.team.presentation.dto.response.UpdateTeamResponse;
import com.tissue.api.team.validator.TeamValidator;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamCommandService {

	private final WorkspaceRepository workspaceRepository;
	private final TeamRepository teamRepository;
	private final TeamValidator teamValidator;

	@Transactional
	public CreateTeamResponse createTeam(
		String workspaceCode,
		CreateTeamRequest request
	) {
		Workspace workspace = findWorkspace(workspaceCode);

		ColorType randomColor = ColorType.getRandomUnusedColor(workspace.getUsedTeamColors());

		Team savedTeam = createTeam(request, workspace, randomColor);

		return CreateTeamResponse.from(savedTeam);
	}

	@Transactional
	public UpdateTeamResponse updateTeam(
		String workspaceCode,
		Long teamId,
		UpdateTeamRequest request
	) {
		Team team = findTeam(teamId, workspaceCode);

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
		Team team = findTeam(teamId, workspaceCode);

		team.updateColor(request.colorType());

		return UpdateTeamColorResponse.from(team);
	}

	@Transactional
	public DeleteTeamResponse deleteTeam(
		String workspaceCode,
		Long teamId
	) {
		Team team = findTeam(teamId, workspaceCode);

		teamValidator.validateTeamIsUsed(team);

		teamRepository.delete(team);

		return DeleteTeamResponse.from(team);
	}

	private Team createTeam(
		CreateTeamRequest request,
		Workspace workspace,
		ColorType color
	) {
		Team team = Team.builder()
			.name(request.name())
			.description(request.description())
			.color(color)
			.workspace(workspace)
			.build();

		return teamRepository.save(team);
	}

	private Workspace findWorkspace(String code) {
		return workspaceRepository.findByCode(code)
			.orElseThrow(() -> new WorkspaceNotFoundException(code));
	}

	private Team findTeam(Long teamId, String code) {
		return teamRepository.findByIdAndWorkspaceCode(teamId, code)
			.orElseThrow(() -> new ResourceNotFoundException(
					String.format(
						"Team was not found with teamId: %d, workspaceCode: %s",
						teamId, code
					)
				)
			);
	}
}
