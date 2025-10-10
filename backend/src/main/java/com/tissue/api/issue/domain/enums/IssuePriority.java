package com.tissue.api.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssuePriority {
	BLOCKER(1), // highest
	MAJOR(2),
	NORMAL(3),
	MINOR(4),
	TRIVIAL(5); // lowest

	private final int level;
}
