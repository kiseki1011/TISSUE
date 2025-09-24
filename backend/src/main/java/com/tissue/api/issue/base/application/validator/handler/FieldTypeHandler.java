package com.tissue.api.issue.base.application.validator.handler;

import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;

import lombok.NonNull;

public interface FieldTypeHandler {

	FieldType type();

	/** Return true if the raw input should be treated as "blank" (e.g., "" for TEXT). */
	default boolean isBlank(Object raw) {
		return (raw instanceof String s) && s.isBlank();
	}

	/** Parse the raw input (JSON-decoded object) into the strongly-typed domain value. */
	Object parse(@NonNull IssueField field, @NonNull Object raw);

	/** Assign the parsed value into the right column(s) of IssueFieldValue. */
	default void assign(@NonNull IssueFieldValue target, @NonNull Object parsed) {
		target.apply(parsed);
	}
}
