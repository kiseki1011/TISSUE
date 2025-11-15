package com.tissue.api.workspace.domain.policy;

import com.tissue.api.workspace.domain.model.Workspace;

public record WorkspacePolicy(
	int maxMemberCount
) {
	public void ensureWithinMemberLimit(Workspace workspace) {
		if (workspace.getMemberCount() >= maxMemberCount) {
			// TODO: WorkspaceMemberLimitException
			throw new RuntimeException("Maximum number of members reached: %d".formatted(maxMemberCount));
		}
	}
}
