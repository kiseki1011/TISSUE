package com.tissue.api.workspace.domain.policy;

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.workspace.domain.model.Workspace;

public record WorkspacePolicy(
	int maxMemberCount
) {
	public void ensureWithinMemberLimit(Workspace workspace) {
		if (workspace.getMemberCount() >= maxMemberCount) {
			throw new BadRequestException("Maximum number of members reached: %d".formatted(maxMemberCount));
		}
	}
}
