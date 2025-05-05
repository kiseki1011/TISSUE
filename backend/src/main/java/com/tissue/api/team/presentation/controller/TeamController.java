package com.tissue.api.team.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.team.presentation.dto.request.CreateTeamRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamColorRequest;
import com.tissue.api.team.presentation.dto.request.UpdateTeamRequest;
import com.tissue.api.team.presentation.dto.response.GetTeamsResponse;
import com.tissue.api.team.presentation.dto.response.TeamResponse;
import com.tissue.api.team.service.command.TeamCommandService;
import com.tissue.api.team.service.query.TeamQueryService;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{code}/teams")
@RequiredArgsConstructor
public class TeamController {

	private final TeamCommandService teamCommandService;
	private final TeamQueryService teamQueryService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<TeamResponse> createTeam(
		@PathVariable String code,
		@Valid @RequestBody CreateTeamRequest request
	) {
		TeamResponse response = teamCommandService.createTeam(code, request);
		return ApiResponse.created("Team created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@PatchMapping("/{teamId}")
	public ApiResponse<TeamResponse> updateTeam(
		@PathVariable String code,
		@PathVariable Long teamId,
		@Valid @RequestBody UpdateTeamRequest request
	) {
		TeamResponse response = teamCommandService.updateTeam(code, teamId, request);
		return ApiResponse.ok("Team updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@PatchMapping("/{teamId}/color")
	public ApiResponse<TeamResponse> updateTeamColor(
		@PathVariable String code,
		@PathVariable Long teamId,
		@Valid @RequestBody UpdateTeamColorRequest request
	) {
		TeamResponse response = teamCommandService.updateTeamColor(code, teamId, request);
		return ApiResponse.ok("Team color updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{teamId}")
	public ApiResponse<Void> deleteTeam(
		@PathVariable String code,
		@PathVariable Long teamId
	) {
		teamCommandService.deleteTeam(code, teamId);
		return ApiResponse.okWithNoContent("Team deleted.");
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping
	public ApiResponse<GetTeamsResponse> getTeams(
		@PathVariable String code
	) {
		GetTeamsResponse response = teamQueryService.getTeams(code);
		return ApiResponse.ok("Teams retrieved.", response);
	}
}
