package com.tissue.issuetype.domain.exception;

import com.tissue.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueTypeErrorCode implements ErrorCode {

	ISSUE_TYPE_NOT_FOUND("Issue type not found"),
	ISSUE_FIELD_NOT_FOUND("Issue field not found"),
	FIELD_OPTION_NOT_FOUND("Field option not found"),

	DUPLICATE_ISSUE_TYPE_NAME("Issue type name already exists in project"),
	DUPLICATE_ISSUE_FIELD_NAME("Issue field name already exists in issue type"),
	DUPLICATE_FIELD_OPTION_NAME("Option label already exists in field"),

	SYSTEM_ISSUE_TYPE_NOT_DELETABLE("Cannot delete system issue type"),
	ISSUE_TYPE_IN_USE("Issue type is currently in use"),
	ISSUE_FIELD_IN_USE("Issue field is currently in use"),
	FIELD_OPTION_IN_USE("Field option is currently in use"),

	OPTION_LIMIT_EXCEEDED("Maximum number of options exceeded"),
	OPTION_REORDER_SIZE_MISMATCH("Number of provided options does not match current options"),
	OPTION_REORDER_DUPLICATE_ID("Duplicate option IDs provided"),
	OPTION_REORDER_UNKNOWN_ID("Provided option ID does not exist in this field"),

	UNSUPPORTED_FIELD_TYPE("Unsupported field type");

	private final String defaultMessage;
}