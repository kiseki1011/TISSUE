package com.tissue.project.domain.exception;

import com.tissue.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectErrorCode implements ErrorCode {

	// Project
	PROJECT_NOT_FOUND("Project not found"),
	PROJECT_ARCHIVED("Cannot modify archived project and its resources"),
	DUPLICATE_PROJECT_KEY("Project key is duplicate in workspace"),
	RESERVED_PROJECT_KEY("Cannot use reserved project key"),

	// Project Member
	INVALID_DEFAULT_JOIN_ROLE("Invalid default join role"),
	PROJECT_MEMBER_NOT_FOUND("Project member not found"),
	PROJECT_MEMBER_ALREADY_EXISTS("Project member already exists"),
	SELF_KICK_NOT_ALLOWED("Cannot kick yourself from project"),
	SELF_ROLE_MODIFICATION_NOT_ALLOWED("Cannot change your own project role");

	private final String defaultMessage;
}
