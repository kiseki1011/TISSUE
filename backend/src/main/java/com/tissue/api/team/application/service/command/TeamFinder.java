package com.tissue.api.team.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.team.domain.model.Team;
import com.tissue.api.team.infrastructure.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamFinder {

	private final TeamRepository teamRepository;

	public Team findTeam(Long teamId, String workspaceCode) {
		return teamRepository.findByIdAndWorkspace_Key(teamId, workspaceCode)
			// TODO: TeamNotFoundException
			.orElseThrow(() -> new RuntimeException(String.format(
				"Team was not found with teamId: %d, workspaceKey: %s",
				teamId, workspaceCode)));
	}
}
