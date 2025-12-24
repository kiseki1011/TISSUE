package com.tissue.security.authorization.project.sprint;

import org.springframework.stereotype.Component;

import com.tissue.security.authorization.project.ProjectSecurityGuard;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.exception.SprintExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintSecurityGuard {

	private final SprintQueryRepository sprintRepository;
	private final ProjectSecurityGuard projectSecurityGuard;

	public boolean isSprintManager(Long sprintId, String projectKey, Long memberId) {
		Sprint sprint = sprintRepository.findByIdAndProject_Key(sprintId, projectKey)
			.orElseThrow(() -> SprintExceptions.notFound(sprintId, projectKey));

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
