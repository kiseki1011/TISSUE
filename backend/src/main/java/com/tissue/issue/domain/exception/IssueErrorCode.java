package com.tissue.issue.domain.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueErrorCode implements ErrorCode {
    ISSUE_NOT_FOUND("Issue not found"),
    INVALID_PARENT_HIERARCHY("Parent hierarchy must be exactly one level above the child issue"),
    STORY_POINT_NOT_ALLOWED("Story points are not supported for this hierarchy"),
    PARENT_REQUIRED("Issues of this hierarchy require a parent and cannot stand alone"),
    PARENT_WORKSPACE_MISMATCH("Parent must belong to the same workspace as the child issue"),
    PARENT_PROJECT_MISMATCH(
            "Cross project parent-child relations are only allowed when the parent is EPIC" + " hierarchy"),
    ISSUE_SELF_REFERENCE("An issue cannot reference itself"),
    TRANSITION_SOURCE_STATE_NOT_MATCH("Issue's current state does not match the required source state for transition"),
    ONLY_INITIAL_STATE_DELETION_ALLOWED("Cannot delete issue that is not in the initial state"),
    CANNOT_DELETE_ISSUE_WITH_CHILDREN("Cannot delete issue that has child issues"),
    DUE_DATE_MUST_BE_FUTURE("Due date must be in the future"),
    INVALID_PERCENTAGE_EXCEPTION("Percentage must be a value of 0 ~ 100"),

    // TODO: I think the next message is more context correct "The actor(requester? submitter?) was
    // not a reviewer"
    REVIEWER_NOT_FOUND("Reviewer not found in issue participants"),

    RELATION_CIRCULAR_DEPENDENCY("Circular dependency detected in the issue relation graph"),
    RELATION_ISSUE_TYPE_MISMATCH("Some relation types require both issues to be of the same issue type"),
    RELATION_ALREADY_EXISTS("A relation already exists between these two issues"),
    RELATION_WORKSPACE_MISMATCH("Both issues in a relation must belong to the same workspace"),

    CUSTOM_FIELD_REQUIRED("Required custom field is missing or empty"),
    CUSTOM_FIELD_TYPE_MISMATCH("Invalid value format for the custom field"),
    UNKNOWN_CUSTOM_FIELD_ID("The provided custom field ID is unknown"),
    UNKNOWN_ENUM_OPTION("Unknown enum option for field"),

    DECIMAL_SCALE_EXCEEDED("Field value exceeds maximum allowed fraction digits"),
    INTEGER_DIGITS_EXCEEDED("Field value exceeds maximum allowed integer digits"),
    MAX_REVIEWERS_EXCEEDED("Maximum number of reviewers reached");

    private final String defaultMessage;
}
