package com.tissue.api.team.application.service.query;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.team.infrastructure.repository.TeamQueryRepository;
import com.tissue.api.team.presentation.dto.response.GetTeamsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamQueryService {

	private final TeamQueryRepository teamQueryRepository;

	@Transactional(readOnly = true)
	public GetTeamsResponse getTeams(String workspaceCode) {
		List<Team> teams = teamQueryRepository.findAllByWorkspace_KeyOrderByCreatedDateAsc(workspaceCode);
		return GetTeamsResponse.from(teams);
	}

	@Transactional(readOnly = true)
	public Set<ColorType> getUsedColors(String workspaceCode) {
		List<Team> teams = teamQueryRepository.findAllByWorkspace_Key(workspaceCode);
		return teams.stream()
			.map(Team::getColor)
			.collect(Collectors.toSet());
	}
}
