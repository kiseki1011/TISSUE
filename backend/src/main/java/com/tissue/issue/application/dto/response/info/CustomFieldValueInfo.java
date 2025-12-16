package com.tissue.issue.application.dto.response.info;

import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.FieldType;

public record CustomFieldValueInfo(
	Long fieldId,
	String fieldLabel,
	FieldType fieldType,
	boolean required,
	Object value
) {
	public static CustomFieldValueInfo from(IssueFieldValue fieldValue) {
		IssueField field = fieldValue.getField();

		Object value = extractValue(fieldValue, field.getFieldType());

		return new CustomFieldValueInfo(
			field.getId(),
			field.getDisplayLabel(),
			field.getFieldType(),
			field.isRequired(),
			value
		);
	}

	private static Object extractValue(IssueFieldValue fv, FieldType type) {
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
