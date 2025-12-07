package com.tissue.api.issue.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StateCategory {
	TODO,
	IN_PROGRESS,
	DONE;

	public boolean isDone() {
		return this == DONE;
	}

	public boolean isInProgress() {
		return this == IN_PROGRESS;
	}

	public boolean isTodo() {
		return this == TODO;
	}

	public boolean isNotTodo() {
		return !isTodo();
	}
}
