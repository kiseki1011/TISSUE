package com.tissue.api.workspace.domain.policy;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspace.domain.model.Workspace;

public record WorkspacePolicy(
	int maxMemberCount
) {
	public void validateMemberLimit(Workspace workspace) {
		if (workspace.getMemberCount() >= maxMemberCount) {
			throw new InvalidOperationException("Maximum number of members reached: %d".formatted(maxMemberCount));
		}
	}
}
