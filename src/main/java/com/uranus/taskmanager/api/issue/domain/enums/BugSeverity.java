package com.uranus.taskmanager.api.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BugSeverity {
	TRIVIAL(1),
	MINOR(2),
	MAJOR(3),
	CRITICAL(5),
	BLOCKER(8),
	DISASTER(13);

	private final int level;

	public boolean isMoreSevereThan(BugSeverity other) {
		return this.level > other.level;
	}
}
