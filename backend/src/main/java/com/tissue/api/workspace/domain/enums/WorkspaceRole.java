package com.tissue.api.workspace.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceRole {

	OWNER(1),
	ADMIN(2),
	MEMBER(3);

	private final int level;

	public boolean isLowerThan(WorkspaceRole other) {
		return this.level > other.getLevel();
	}

	public boolean isEqualOrHigherThan(WorkspaceRole other) {
		return this.level <= other.getLevel();
	}
}
