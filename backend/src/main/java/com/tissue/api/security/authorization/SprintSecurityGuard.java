package com.tissue.api.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.exception.SprintNotFoundException;
import com.tissue.api.sprint.domain.port.out.SprintQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintSecurityGuard {

	private final SprintQueryRepository sprintRepository;
	private final ProjectSecurityGuard projectSecurityGuard;

	public boolean isSprintManager(Long sprintId, Long memberId) {

		Sprint sprint = sprintRepository.findById(sprintId)
			.orElseThrow(() -> new SprintNotFoundException(sprintId));

		if (sprint.getCreatedBy().equals(memberId)) {
			return true;
		}

		return projectSecurityGuard.isAdmin(
			sprint.getWorkspaceKey(),
			sprint.getProjectKey(),
			memberId
		);
	}
}
