package com.tissue.api.team.service.command;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.repository.TeamRepository;

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
