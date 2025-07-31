package com.tissue.api.issue.base.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tissue.api.common.dto.FieldErrorDto;
import com.tissue.api.common.exception.type.FieldValidationException;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueFieldDefinition;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldValueRepository;

import lombok.RequiredArgsConstructor;

/**
 * An improved version of IssueFieldSchemaValidtor.
 * Throws FieldValidationException with List<FieldErrorDto>.
 */
@Service
@RequiredArgsConstructor
public class IssueCustomFieldValidator {

	private final IssueFieldRepository issueFieldRepository;
	private final IssueFieldValueRepository issueFieldValueRepository;

	// TODO: Find a way to improve or simplify logic.
	public List<IssueFieldValue> validateAndExtract(
		Map<String, Object> input,
		Issue issue
	) {
		List<IssueFieldDefinition> fields = issueFieldRepository.findByIssueType(issue.getIssueType());
		List<IssueFieldValue> values = new ArrayList<>();
		List<FieldErrorDto> errors = new ArrayList<>();

		for (IssueFieldDefinition field : fields) {
			Object value = input.get(field.getKey());

			FieldErrorDto error = validateField(field, value);
			if (error != null) {
				errors.add(error);
				continue;
			}

			values.add(IssueFieldValue.of(issue, field, value));
		}

		if (!errors.isEmpty()) {
			throw new FieldValidationException("Custom field validation failed.", errors);
		}

		return values;
	}

	public List<IssueFieldValue> validateAndApplyPartialUpdate(
		Map<String, Object> input,
		Issue issue
	) {
		// Load existing values
		List<IssueFieldValue> existingValues = issueFieldValueRepository.findByIssue(issue);
		Map<String, IssueFieldValue> existingMap = existingValues.stream()
			.collect(Collectors.toMap(val -> val.getField().getKey(), Function.identity()));

		// Load definitions
		List<IssueFieldDefinition> fields = issueFieldRepository.findByIssueType(issue.getIssueType());
		Map<String, IssueFieldDefinition> fieldMap = fields.stream()
			.collect(Collectors.toMap(IssueFieldDefinition::getKey, Function.identity()));

		List<IssueFieldValue> result = new ArrayList<>();
		List<FieldErrorDto> errors = new ArrayList<>();

		for (Map.Entry<String, Object> entry : input.entrySet()) {
			String key = entry.getKey();
			Object newValue = entry.getValue();

			IssueFieldDefinition field = fieldMap.get(key);
			if (field == null) {
				errors.add(new FieldErrorDto(key, valueOrNull(newValue), "Unknown custom field."));
				continue;
			}

			FieldErrorDto error = validateField(field, newValue);
			if (error != null) {
				errors.add(error);
				continue;
			}

			IssueFieldValue fieldValue = existingMap.get(key);
			if (fieldValue == null) {
				fieldValue = IssueFieldValue.of(issue, field, newValue);
			} else {
				fieldValue.updateValue(newValue);
			}

			result.add(fieldValue);
		}

		if (!errors.isEmpty()) {
			throw new FieldValidationException("Custom field validation failed.", errors);
		}

		return result;
	}

	private FieldErrorDto validateField(IssueFieldDefinition field, Object value) {
		if (field.isRequired() && (value == null || value.toString().isBlank())) {
			return error(field, value, "This field is required.");
		}
		if (value == null) {
			return null;
		}

		return switch (field.getFieldType()) {
			case TEXT -> validateText(field, value);
			case ENUM -> validateEnum(field, value);
			case NUMBER -> validateNumber(field, value);
			case DATE -> validateDate(field, value);
			default -> error(field, value, "Unsupported field type.");
		};
	}

	private FieldErrorDto validateText(IssueFieldDefinition field, Object value) {
		if (!(value instanceof String)) {
			return error(field, value, "Must be a string.");
		}
		return null;
	}

	private FieldErrorDto validateEnum(IssueFieldDefinition field, Object value) {
		if (!(value instanceof String s)) {
			return error(field, value, "Must be a string.");
		}
		if (!field.getAllowedOptions().contains(s)) {
			return error(field, value, "Invalid value. Allowed: " + field.getAllowedOptions());
		}
		return null;
	}

	private FieldErrorDto validateNumber(IssueFieldDefinition field, Object value) {
		if (!(value instanceof Number)) {
			return error(field, value, "Must be a number.");
		}
		return null;
	}

	private FieldErrorDto validateDate(IssueFieldDefinition field, Object value) {
		try {
			LocalDate.parse(value.toString());
			return null;
		} catch (DateTimeParseException e) {
			return error(field, value, "Must be a valid date.");
		}
	}

	private FieldErrorDto error(IssueFieldDefinition field, Object value, String message) {
		return new FieldErrorDto(field.getKey(), valueOrNull(value), message);
	}

	private String valueOrNull(Object value) {
		return value == null ? FieldErrorDto.NULL_PLACEHOLDER : value.toString();
	}
}
