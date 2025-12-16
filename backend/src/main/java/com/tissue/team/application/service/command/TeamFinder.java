package com.tissue.team.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.team.domain.model.Team;
import com.tissue.team.infrastructure.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamFinder {

	private final TeamRepository teamRepository;

	public Team findByIdAndWorkspaceKey(Long teamId, String workspaceCode) {
		return teamRepository.findByIdAndWorkspace_Key(teamId, workspaceCode)
			// TODO: TeamNotFoundException
			.orElseThrow(() -> new RuntimeException(String.format(
				"Team was not found with teamId: %d, workspaceKey: %s",
				teamId, workspaceCode)));
	}
}
