package com.tissue.api.workspacemember.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceRole {
	OWNER(5),
	ADMIN(4),
	MANAGER(3),
	MEMBER(2),
	VIEWER(1);

	private final int level;

	public boolean isLowerThan(WorkspaceRole role) {
		return level < role.getLevel();
	}

	public boolean isHigherThan(WorkspaceRole role) {
		return level > role.getLevel();
	}
}
