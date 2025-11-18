package com.tissue.api.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRole {

	ADMIN(3),
	MEMBER(2),
	VIEWER(1);
	
	// TODO: GEUST 고려

	private final int level;

	public boolean isLowerThan(ProjectRole role) {
		return level < role.getLevel();
	}

	public boolean isHigherThan(ProjectRole role) {
		return level > role.getLevel();
	}
}
