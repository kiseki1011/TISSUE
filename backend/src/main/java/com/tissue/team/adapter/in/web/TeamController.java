package com.tissue.team.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tissue.team.adapter.in.web.request.CreateTeamRequest;
import com.tissue.team.adapter.in.web.request.UpdateTeamRequest;
import com.tissue.team.application.dto.response.GetTeams;
import com.tissue.team.application.dto.response.TeamCreateResponse;
import com.tissue.team.application.dto.response.TeamDetail;
import com.tissue.team.application.port.in.TeamUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/teams")
@RequiredArgsConstructor
public class TeamController {

	private final TeamUseCase teamUseCase;

	@PostMapping
	public ResponseEntity<TeamCreateResponse> createTeam(
		@PathVariable String workspaceKey,
		@Valid @RequestBody CreateTeamRequest request
	) {
		var command = request.toCommand(workspaceKey);
		TeamCreateResponse response = teamUseCase.create(command);

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{teamId}")
			.buildAndExpand(response.teamId())
			.toUri();

		return ResponseEntity.created(location)
			.body(response);
	}

	@PatchMapping("/{teamId}")
	public ResponseEntity<Void> updateTeam(
		@PathVariable String workspaceKey,
		@PathVariable Long teamId,
		@Valid @RequestBody UpdateTeamRequest request
	) {
		var command = request.toCommand(workspaceKey, teamId);
		teamUseCase.update(command);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{teamId}")
	public ResponseEntity<Void> deleteTeam(
		@PathVariable String workspaceKey,
		@PathVariable Long teamId
	) {
		teamUseCase.delete(workspaceKey, teamId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{teamId}")
	public ResponseEntity<TeamDetail> getTeamDetail(
		@PathVariable String workspaceKey,
		@PathVariable Long teamId
	) {
		TeamDetail response = teamUseCase.getTeam(workspaceKey, teamId);
		return ResponseEntity.ok(response);
	}

	@GetMapping
	public ResponseEntity<GetTeams> getTeams(
		@PathVariable String workspaceKey
	) {
		GetTeams response = teamUseCase.getTeams(workspaceKey);
		return ResponseEntity.ok(response);
	}
}
