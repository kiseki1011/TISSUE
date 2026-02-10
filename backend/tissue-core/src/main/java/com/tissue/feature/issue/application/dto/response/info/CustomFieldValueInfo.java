package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import org.jspecify.annotations.Nullable;

public record CustomFieldValueInfo(
        Long fieldId,
        String fieldLabel,
        IssueFieldType issueFieldType,
        boolean required,
        @Nullable Object value) {

    public static CustomFieldValueInfo from(IssueFieldValue fieldValue) {
        IssueField field = fieldValue.getField();
        Object value = extractValue(fieldValue, field.getIssueFieldType());

        return new CustomFieldValueInfo(
                field.getId(), field.getName(), field.getIssueFieldType(), field.isRequired(), value);
    }

    private static @Nullable Object extractValue(IssueFieldValue fv, IssueFieldType type) {
        if (!fv.isValuePresent()) {
            return null;
        }

        return switch (type) {
            case TEXT -> fv.getStringValue();
            case INTEGER -> fv.getIntegerValue();
            case DECIMAL -> fv.getDecimalValue();
            case TIMESTAMP -> fv.getTimestampValue();
            case DATE -> fv.getDateValue();
            case BOOLEAN -> fv.getBooleanValue();
            case ENUM -> EnumOptionInfo.of(fv.getEnumOption());
        };
    }
}
