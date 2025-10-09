package com.tissue.api.workflow.domain.gaurd;

public enum GuardType {
	ASSIGNEE_REQUIRED,
	REVIEWER_REQUIRED,
	AUTHOR_ONLY,
	ALL_REVIEWERS_APPROVED,
	MIN_STORY_POINT,
	ALL_SUBTASKS_DONE;
	// 기타 등...
}
