package com.tissue.api.team.application.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.team.infrastructure.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamReader {

	private final TeamRepository teamRepository;

	public Team findTeam(Long teamId, String workspaceCode) {
		return teamRepository.findByIdAndWorkspaceCode(teamId, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				"Team was not found with teamId: %d, workspaceCode: %s",
				teamId, workspaceCode)));
	}
}
