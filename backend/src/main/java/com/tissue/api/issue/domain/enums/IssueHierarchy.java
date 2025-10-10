package com.tissue.api.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueHierarchy {

	EPIC(1), // highest
	STORY(2),
	SUBTASK(3),
	MICROTASK(4); // lowest

	private final int level;

	public boolean isOneLevelHigher(IssueHierarchy hierarchy) {
		return this.level == hierarchy.level - 1;
	}
}
