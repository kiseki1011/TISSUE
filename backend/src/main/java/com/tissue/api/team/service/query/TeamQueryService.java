package com.tissue.api.team.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.repository.TeamRepository;
import com.tissue.api.team.presentation.dto.response.GetTeamsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQueryService {

	private final TeamRepository teamRepository;

	@Transactional(readOnly = true)
	public GetTeamsResponse getTeams(String workspaceCode) {

		List<Team> teams = teamRepository.findAllByWorkspaceCodeOrderByCreatedDateAsc(workspaceCode);

		return GetTeamsResponse.from(teams);
	}

	@Transactional(readOnly = true)
	public Team findTeam(Long teamId, String code) {
		return teamRepository.findByIdAndWorkspaceCode(teamId, code)
			.orElseThrow(() -> new ResourceNotFoundException(String.format(
				"Team was not found with teamId: %d, workspaceCode: %s",
				teamId, code)));
	}
}
