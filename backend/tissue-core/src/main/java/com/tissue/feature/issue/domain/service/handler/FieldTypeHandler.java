package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.exception.CustomFieldTypeMismatchException;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;

public interface FieldTypeHandler {

    IssueFieldType type();

    /**
     * Parse the raw input into the strongly-typed domain value.
     */
    @Nullable
    Object parse(IssueField field, @Nullable Object raw);

    /**
     * Convert domain value to a JSON-safe value for JSONB storage.
     */
    @Nullable
    Object toJsonValue(@Nullable Object domainValue);

    /**
     * Restore domain value from a JSON-deserialized value.
     */
    @Nullable
    Object fromJsonValue(@Nullable Object jsonValue);

    @Nullable
    default <T> T convert(ConversionService cs, @Nullable Object raw, Class<T> targetType, IssueField field) {
        if (raw == null) {
            return null;
        }

        try {
            return cs.convert(raw, targetType);
        } catch (ConversionFailedException ex) {
            throw new CustomFieldTypeMismatchException(field.getId(), field.getName(), field.getIssueFieldType(), raw);
        } catch (ConverterNotFoundException ex) {
            throw new IllegalStateException(
                    "No converter found for: " + raw.getClass().getSimpleName() + " to " + targetType.getSimpleName());
        }
    }

    /**
     * Return true if the raw input should be treated as blank.
     */
    default boolean isBlank(@Nullable Object raw) {
        return (raw == null) || (raw instanceof String s) && s.isBlank();
    }
}
