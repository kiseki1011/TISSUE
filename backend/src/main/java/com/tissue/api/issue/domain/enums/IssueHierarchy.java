package com.tissue.api.issue.domain.enums;

import lombok.Getter;

@Getter
public enum IssueHierarchy {

	EPIC(1), // highest
	STORY(2),
	SUBTASK(3),
	MICROTASK(4); // lowest

	private final int level;

	IssueHierarchy(int level) {
		this.level = level;
	}

	// TODO: move this to Issue. from Issue call the isOneLevelAbove() method.
	//  - i dont think enums should have business logic. they should only provide helper methods.
	public boolean isInvalidParentFor(IssueHierarchy hierarchy) {
		return !isOneLevelHigher(hierarchy);
	}

	public boolean isOneLevelHigher(IssueHierarchy hierarchy) {
		return this.level == hierarchy.level - 1;
	}
}
