package com.tissue.issue.domain.service.handler;

import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;

import lombok.NonNull;

public interface FieldTypeHandler {

	IssueFieldType type();

	/**
	 * Return true if the raw input should be treated as "blank" (e.g., "" for TEXT).
	 */
	default boolean isBlank(Object raw) {
		return (raw == null) || (raw instanceof String s) && s.isBlank();
	}

	/**
	 * Parse the raw input (JSON-decoded object) into the strongly-typed domain value.
	 */
	Object parse(@NonNull IssueField field, @NonNull Object raw);

	/**
	 * Assign the parsed value into the right column(s) of IssueFieldValue.
	 */
	default void assign(@NonNull IssueFieldValue target, @NonNull Object parsed) {
		target.apply(parsed);
	}
}
