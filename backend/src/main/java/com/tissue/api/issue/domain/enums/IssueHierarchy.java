package com.tissue.api.issue.domain.enums;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueHierarchy {

	EPIC(1), // highest
	STANDARD(2),
	SUBTASK(3),
	MICROTASK(4); // lowest

	private final int level;

	public boolean isEpic() {
		return this == EPIC;
	}

	public boolean isNotEpic() {
		return !isEpic();
	}

	// TODO: 아래는 정책이나 비즈니스 로직에 가까운 것들인데, 여기(enum)에 정의해서 사용하는게 괜찮나?
	public static List<IssueHierarchy> getStoryPointModifiable() {
		return List.of(STANDARD);
	}

	public static List<IssueHierarchy> getParentRequired() {
		return List.of(SUBTASK, MICROTASK);
	}

	public static List<IssueHierarchy> getCrossProjectParentAllowed() {
		return List.of(STANDARD);
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

	public boolean cannotHaveCrossProjectParent() {
		return !getCrossProjectParentAllowed().contains(this);
	}

	public boolean cannotModifyStoryPoint() {
		return !canModifyStoryPoint();
	}

	public boolean canModifyStoryPoint() {
		return getStoryPointModifiable().contains(this);
	}

	public boolean canUseStoryPoint() {
		return this == EPIC || this == STANDARD;
	}
}
