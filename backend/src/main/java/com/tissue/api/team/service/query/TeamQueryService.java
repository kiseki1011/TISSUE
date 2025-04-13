package com.tissue.api.team.service.query;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.repository.TeamQueryRepository;
import com.tissue.api.team.presentation.dto.response.GetTeamsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQueryService {

	private final TeamQueryRepository teamQueryRepository;

	@Transactional(readOnly = true)
	public GetTeamsResponse getTeams(String workspaceCode) {

		List<Team> teams = teamQueryRepository.findAllByWorkspaceCodeOrderByCreatedDateAsc(workspaceCode);

		return GetTeamsResponse.from(teams);
	}
}
