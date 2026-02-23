package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issue.domain.exception.CustomFieldTypeMismatchException;
import com.tissue.feature.issue.domain.exception.IssueFieldConverterNotFoundException;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;

public interface FieldTypeHandler {

    IssueFieldType type();

    /**
     * Return true if the raw input should be treated as blank.
     */
    default boolean isBlank(@Nullable Object raw) {
        return (raw == null) || (raw instanceof String s) && s.isBlank();
    }

    /**
     * Parse the raw input (JSON-decoded object) into the strongly-typed domain value.
     */
    @Nullable
    Object parse(IssueField field, @Nullable Object raw);

    /**
     * Assign the parsed value into the right column of IssueFieldValue.
     */
    void assign(IssueFieldValue target, @Nullable Object parsed);

    /**
     * Retrieve the typed value from the right column of IssueFieldValue.
     */
    @Nullable
    Object getValueFrom(IssueFieldValue target);

    default <T> @Nullable T convert(ConversionService cs, @Nullable Object raw, Class<T> targetType, IssueField field) {

        if (raw == null) {
            return null;
        }

        try {
            return cs.convert(raw, targetType);
        } catch (ConversionFailedException ex) {
            throw new CustomFieldTypeMismatchException(field.getId(), field.getName(), field.getIssueFieldType(), raw);
        } catch (ConverterNotFoundException ex) {
            throw new IssueFieldConverterNotFoundException(
                    "No converter found for: " + raw.getClass().getSimpleName() + " to " + targetType.getSimpleName());
        }
    }
}
