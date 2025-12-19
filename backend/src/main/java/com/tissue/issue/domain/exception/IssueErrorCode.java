package com.tissue.issue.domain.exception;

import com.tissue.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueErrorCode implements ErrorCode {

	ISSUE_NOT_FOUND("Issue not found"),

	INVALID_PARENT_HIERARCHY("Parent hierarchy must be exactly one level above the child issue"),

	STORY_POINT_NOT_ALLOWED("Story points are not supported for this hierarchy level"),

	PARENT_REQUIRED("Issues of this hierarchy level require a parent and cannot stand alone"),

	PARENT_WORKSPACE_MISMATCH("Parent must belong to the same workspace as the child issue"),

	PARENT_PROJECT_MISMATCH("Cross project parent-child relations are only allowed when the parent is EPIC level"),

	TRANSITION_SOURCE_STATE_NOT_MATCH("Issue's current state does not match the required source state for transition"),

	ISSUE_SELF_REFERENCE("An issue cannot reference itself"),

	RELATION_CIRCULAR_DEPENDENCY("Circular dependency detected in the issue relation graph"),

	RELATION_ISSUE_TYPE_MISMATCH("Some relation types require both issues to be of the same issue type"),

	RELATION_ALREADY_EXISTS("A relation already exists between these two issues"),

	RELATION_WORKSPACE_MISMATCH("Both issues in a relation must belong to the same workspace"),

	ONLY_INITIAL_STATE_DELETION_ALLOWED("Cannot delete issue that is not in the initial state"),

	CANNOT_DELETE_ISSUE_WITH_CHILDREN("Cannot delete issue that has child issues"),

	DUE_DATE_MUST_BE_FUTURE("Due date must be in the future"),

	CUSTOM_FIELD_REQUIRED("Required custom field is missing or empty"),

	UNKNOWN_CUSTOM_FIELD_ID("The provided custom field ID is unknown"),

	CUSTOM_FIELD_TYPE_MISMATCH("Invalid value format for the custom field");

	private final String defaultMessage;
}
