package com.tissue.api.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Difficulty {
	TRIVIAL(1),
	SIMPLE(2),
	NORMAL(3),
	HARD(5),
	COMPLEX(8),
	INTENSIVE(13),
	MASSIVE(21);

	private final int value;
}
