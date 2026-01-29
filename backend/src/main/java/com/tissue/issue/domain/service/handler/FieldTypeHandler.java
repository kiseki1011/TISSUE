package com.tissue.issue.domain.service.handler;

import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import org.jspecify.annotations.Nullable;

// TODO: The whole FieldTypeHandler implementation design needs refactoring.
public interface FieldTypeHandler {

    IssueFieldType type();

    /** Return true if the raw input should be treated as "blank" (e.g., "" for TEXT). */
    default boolean isBlank(@Nullable Object raw) {
        return (raw == null) || (raw instanceof String s) && s.isBlank();
    }

    /** Parse the raw input (JSON-decoded object) into the strongly-typed domain value. */
    @Nullable
    Object parse(IssueField field, @Nullable Object raw);

    /** Assign the parsed value into the right column(s) of IssueFieldValue. */
    default void assign(IssueFieldValue target, @Nullable Object parsed) {
        target.apply(parsed);
    }
}
