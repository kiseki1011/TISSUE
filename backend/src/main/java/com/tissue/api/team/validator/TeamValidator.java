package com.tissue.api.team.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.team.domain.model.Team;
import com.tissue.api.team.infrastructure.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TeamValidator {

	private final TeamRepository teamRepository;

	public void ensureDeletable(Team team) {
		if (teamRepository.existsByWorkspaceMembers(team)) {
			// TODO: TeamCurrentlyUsedException, 더 좋은 이름이 있을까?
			//  - 메세지에는 사용 중이라 삭제 불가하다는 내용 필요
			throw new RuntimeException(
				String.format(
					"There is a workspace member that belongs to this team. teamId: %d, name: %s",
					team.getId(), team.getName()
				)
			);
		}
	}
}
