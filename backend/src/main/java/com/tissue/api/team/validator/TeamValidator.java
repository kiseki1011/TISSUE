package com.tissue.api.team.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.position.exception.PositionInUseException;
import com.tissue.api.team.domain.Team;
import com.tissue.api.team.domain.respository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamValidator {

	private final TeamRepository teamRepository;

	public void validateTeamIsUsed(Team team) {
		if (teamRepository.existsByWorkspaceMembers(team)) {
			throw new PositionInUseException();
		}
	}
}
