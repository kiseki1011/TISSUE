package com.tissue.security.authorization.project.sprint;

import static com.tissue.project.domain.enums.ProjectRole.*;

import org.springframework.stereotype.Component;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.sprint.application.port.out.SprintQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintSecurityGuard {

	private final SprintQueryRepository sprintRepository;
	// private final ProjectSecurityGuard projectSecurityGuard;

	public boolean canEditSprint(String workspaceKey, String projectKey, Long sprintId,
		MemberUserDetails userDetails) {
		// TODO: should i call projectSecurityGuard.isAdmin instead of hasProjectRole?
		return userDetails.hasProjectRole(workspaceKey, projectKey, ADMIN)
			|| isSprintCreator(projectKey, sprintId, userDetails);
	}

	private Boolean isSprintCreator(String projectKey, Long sprintId, MemberUserDetails userDetails) {
		return sprintRepository.findByIdAndProject_Key(sprintId, projectKey)
			.map(sprint -> sprint.getCreatedBy().equals(userDetails.getMemberId()))
			.orElse(false);
	}
}
