package com.tissue.api.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceType {
	ISSUE,
	ISSUE_COMMENT,
	REVIEW_COMMENT,
	REVIEW,
	SPRINT,
	WORKSPACE,
	WORKSPACE_MEMBER
}
