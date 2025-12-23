package com.tissue.position.domain.exception;

import static com.tissue.global.exception.ContextKeys.*;
import static com.tissue.position.domain.exception.PositionErrorCode.*;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.position.domain.Position;

public class PositionExceptions {

	private PositionExceptions() {
	}

	public static ResourceNotFoundException notFound(Long positionId, String workspaceKey) {
		return new ResourceNotFoundException(POSITION_NOT_FOUND)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(POSITION_ID, positionId);
	}

	public static ResourceConflictException duplicateName(String positionName, String workspaceKey) {
		return new ResourceConflictException(DUPLICATE_POSITION_NAME)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(POSITION_NAME, positionName);
	}

	public static BadRequestException inUse(Position position) {
		return new BadRequestException(POSITION_IN_USE)
			.addContext(WORKSPACE_KEY, position.getWorkspaceKey())
			.addContext(POSITION_ID, position.getId())
			.addContext(POSITION_NAME, position.getDisplayName());
	}
}
