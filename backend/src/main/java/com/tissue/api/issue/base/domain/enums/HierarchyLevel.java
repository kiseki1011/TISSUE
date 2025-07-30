package com.tissue.api.issue.base.domain.enums;

import lombok.Getter;

@Getter
public enum HierarchyLevel {

	ONE(1), // lowest
	TWO(2),
	THREE(3),
	FOUR(4),
	FIVE(5); // highest

	private final int level;

	HierarchyLevel(int level) {
		this.level = level;
	}

	public boolean isOneLevelAbove(HierarchyLevel other) {
		return this.level == other.level + 1;
	}
}
