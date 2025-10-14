package com.tissue.api.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StateCategory {

	TODO,
	IN_PROGRESS,
	DONE;

	public static StateCategory derive(boolean initial, boolean terminal) {
		if (initial) {
			return TODO;
		}
		if (terminal) {
			return DONE;
		}

		return IN_PROGRESS;
	}
}
