package com.tissue.project.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectRole {

	ADMIN(1),
	MEMBER(2),
	VIEWER(3);

	private final int level;

	public boolean isEqualOrHigherThan(ProjectRole other) {
		return this.level <= other.getLevel();
	}
}
