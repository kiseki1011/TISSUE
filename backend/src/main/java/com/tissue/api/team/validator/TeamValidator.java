package com.tissue.api.team.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.team.domain.model.Team;
import com.tissue.api.team.infrastructure.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamValidator {

	private final TeamRepository teamRepository;

	public void validateTeamIsUsed(Team team) {
		if (teamRepository.existsByWorkspaceMembers(team)) {
			throw new InvalidOperationException(
				String.format(
					"There is a workspace member that belongs to this team. teamId: %d, name: %s",
					team.getId(), team.getName()
				)
			);
		}
	}
}
