package com.tissue.api.issue.base.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssuePriority {
	EMERGENCY(6),
	HIGHEST(5),
	HIGH(4),
	MEDIUM(3),
	LOW(2),
	LOWEST(1);

	private final int level;
}
