package com.tissue.api.issue.base.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueFieldDefinition;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;
import com.tissue.api.issue.base.domain.model.IssueTypeDefinition;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;

import lombok.RequiredArgsConstructor;

// TODO: Im using InvalidOperationException, but should i make or use a better custom exception?
@Service
@RequiredArgsConstructor
public class IssueFieldSchemaValidator {

	private final IssueFieldRepository issueFieldRepository;

	public List<IssueFieldValue> validateAndExtract(
		Map<String, Object> input,
		IssueTypeDefinition issueType,
		Issue issue
	) {
		List<IssueFieldDefinition> fields = issueFieldRepository.findByIssueType(issueType);
		List<IssueFieldValue> values = new ArrayList<>();

		for (IssueFieldDefinition field : fields) {
			Object value = input.get(field.getKey());

			// Required field check
			if (field.isRequired() && (value == null || value.toString().isBlank())) {
				throw new InvalidOperationException("Field '%s' is required.".formatted(field.getLabel()));
			}
			if (value == null) {
				continue;
			}

			// Validate value type and constraints
			validateFieldValue(field, value);

			values.add(IssueFieldValue.of(issue, field, value));
		}

		return values;
	}

	private void validateFieldValue(IssueFieldDefinition field, Object value) {
		switch (field.getFieldType()) {
			case TEXT -> validateTextField(field, value);
			case ENUM -> validateEnumField(field, value);
			case NUMBER -> validateNumberField(field, value);
			case DATE -> validateDateField(field, value);
			default -> throw new InvalidOperationException("Unsupported field type: " + field.getFieldType());
		}
	}

	private void validateTextField(IssueFieldDefinition field, Object value) {
		if (!(value instanceof String)) {
			throw new InvalidOperationException("Field '%s' must be a string.".formatted(field.getLabel()));
		}
	}

	private void validateEnumField(IssueFieldDefinition field, Object value) {
		if (!(value instanceof String)) {
			throw new InvalidOperationException("Field '%s' must be a string enum.".formatted(field.getLabel()));
		}
		if (!field.getAllowedOptions().contains(value.toString())) {
			throw new InvalidOperationException("Invalid option for '%s'. Allowed: %s"
				.formatted(field.getLabel(), field.getAllowedOptions()));
		}
	}

	private void validateNumberField(IssueFieldDefinition field, Object value) {
		if (!(value instanceof Number)) {
			throw new InvalidOperationException("Field '%s' must be a number.".formatted(field.getLabel()));
		}
	}

	// TODO: Should i return the parsed value?
	private void validateDateField(IssueFieldDefinition field, Object value) {
		try {
			LocalDate.parse(value.toString());
		} catch (DateTimeParseException e) {
			throw new InvalidOperationException("Field '%s' must be a valid date.".formatted(field.getLabel()));
		}
	}
}
