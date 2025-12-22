package com.tissue.position.domain.exception;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.position.domain.exception.PositionErrorCode.*;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class PositionExceptions {

	private PositionExceptions() {
	}

	public static ResourceNotFoundException notFound(Long positionId, String workspaceKey) {
		return new ResourceNotFoundException(POSITION_NOT_FOUND)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(POSITION_ID, positionId);
	}
}
