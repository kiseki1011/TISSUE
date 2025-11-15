package com.tissue.api.issue.domain.enums;

import java.util.List;

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

	public static List<IssueHierarchy> getStoryPointModifiable() {
		return List.of(STORY);
	}

	public static List<IssueHierarchy> getParentRequired() {
		return List.of(SUBTASK, MICROTASK);
	}

	public boolean canBeParentOf(IssueHierarchy hierarchy) {
		return this.level == hierarchy.level - 1;
	}

	public boolean cannotBeParentOf(IssueHierarchy hierarchy) {
		return !canBeParentOf(hierarchy);
	}

	public boolean mustHaveParent() {
		return getParentRequired().contains(this);
	}

	public boolean cannotHaveParent() {
		return this == EPIC;
	}

	public boolean cannotModifyStoryPoint() {
		return !canModifyStoryPoint();
	}

	public boolean canModifyStoryPoint() {
		return getStoryPointModifiable().contains(this);
	}

	public boolean canUseStoryPoint() {
		return this == EPIC || this == STORY;
	}
}
