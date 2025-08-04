package com.tissue.api.issue.base.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldValueRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueFieldSchemaValidator {

	private final IssueFieldRepository issueFieldRepository;
	private final IssueFieldValueRepository issueFieldValueRepository;

	public List<IssueFieldValue> validateAndExtract(
		Map<String, Object> input,
		Issue issue
	) {
		List<IssueField> fields = issueFieldRepository.findByIssueType(issue.getIssueType());
		List<IssueFieldValue> values = new ArrayList<>();

		for (IssueField field : fields) {
			Object value = input.get(field.getKey());

			validateSingleField(field, value);

			values.add(IssueFieldValue.of(issue, field, value));
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

		// Load field definitions
		List<IssueField> issueFields = issueFieldRepository.findByIssueType(issue.getIssueType());
		Map<String, IssueField> fieldMap = issueFields.stream()
			.collect(Collectors.toMap(IssueField::getKey, Function.identity()));

		List<IssueFieldValue> result = new ArrayList<>();

		for (Map.Entry<String, Object> entry : input.entrySet()) {
			String key = entry.getKey();
			Object newValue = entry.getValue();

			IssueField field = fieldMap.get(key);
			if (field == null) {
				throw new InvalidCustomFieldException("Unknown custom field: '%s'".formatted(key));
			}

			validateSingleField(field, newValue);

			IssueFieldValue fieldValue = existingMap.get(key);
			if (fieldValue == null) {
				fieldValue = IssueFieldValue.of(issue, field, newValue);
			} else {
				fieldValue.updateValue(newValue);
			}

			result.add(fieldValue);
		}

		return result;
	}

	private void validateSingleField(IssueField field, Object value) {
		if (field.isRequired() && (value == null || value.toString().isBlank())) {
			throw new InvalidCustomFieldException("Field '%s' is required.".formatted(field.getKey()));
		}
		if (value == null) {
			return;
		}

		switch (field.getFieldType()) {
			case TEXT -> validateTextField(field, value);
			case ENUM -> validateEnumField(field, value);
			case NUMBER -> validateNumberField(field, value);
			case DATE -> validateDateField(field, value);
			default -> throw new InvalidCustomFieldException("Unsupported field type: " + field.getKey());
		}
	}

	private void validateTextField(IssueField field, Object value) {
		if (!(value instanceof String)) {
			throw new InvalidCustomFieldException("Field '%s' must be a string.".formatted(field.getKey()));
		}
	}

	private void validateEnumField(IssueField field, Object value) {
		if (!(value instanceof String)) {
			throw new InvalidCustomFieldException("Field '%s' must be a string enum.".formatted(field.getKey()));
		}
		if (!field.getAllowedOptions().contains(value.toString())) {
			throw new InvalidCustomFieldException("Invalid option for '%s'. Allowed: %s"
				.formatted(field.getKey(), field.getAllowedOptions()));
		}
	}

	private void validateNumberField(IssueField field, Object value) {
		if (!(value instanceof Number)) {
			throw new InvalidCustomFieldException("Field '%s' must be a number.".formatted(field.getKey()));
		}
	}

	private void validateDateField(IssueField field, Object value) {
		try {
			LocalDate.parse(value.toString());
		} catch (DateTimeParseException e) {
			throw new InvalidCustomFieldException("Field '%s' must be a valid date.".formatted(field.getKey()));
		}
	}
}
