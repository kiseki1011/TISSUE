package com.tissue.team.domain.exception;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.team.domain.exception.TeamErrorCode.*;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.team.domain.Team;

public class TeamExceptions {

	private TeamExceptions() {
	}

	public static ResourceNotFoundException notFound(Long teamId, String workspaceKey) {
		return new ResourceNotFoundException(TEAM_NOT_FOUND)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(TEAM_ID, teamId);
	}

	public static ResourceConflictException duplicateName(String teamName, String workspaceKey) {
		return new ResourceConflictException(DUPLICATE_TEAM_NAME)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(TEAM_NAME, teamName);
	}

	public static BadRequestException inUse(Team team) {
		return new BadRequestException(TEAM_IN_USE)
			.addContext(WORKSPACE_KEY, team.getWorkspaceKey())
			.addContext(TEAM_ID, team.getId())
			.addContext(TEAM_NAME, team.getDisplayName());
	}
}
