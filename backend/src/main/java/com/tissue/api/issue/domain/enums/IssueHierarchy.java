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

	public boolean canBeParentOf(IssueHierarchy hierarchy) {
		return this.level == hierarchy.level - 1;
	}

	public boolean cannotBeParentOf(IssueHierarchy hierarchy) {
		return this.level != hierarchy.level - 1;
	}

	public boolean mustHaveParent() {
		return this == SUBTASK || this == MICROTASK;
	}

	public boolean cannotHaveParent() {
		return this == EPIC;
	}

	public boolean cannotModifyStoryPoint() {
		return this != STORY;
	}

	public boolean canUseStoryPoint() {
		return this == EPIC || this == STORY;
	}
}
