package com.tissue.api.workspace.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceRole {

	OWNER(3),
	ADMIN(2),
	MEMBER(1);

	private final int level;

	public boolean isLowerThan(WorkspaceRole role) {
		return level < role.getLevel();
	}

	public boolean isHigherThan(WorkspaceRole role) {
		return level > role.getLevel();
	}
}
