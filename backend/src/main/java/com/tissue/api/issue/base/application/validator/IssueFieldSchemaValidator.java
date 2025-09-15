package com.tissue.api.issue.base.application.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldValueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldSchemaValidator {

	private final IssueFieldRepository issueFieldRepo;
	private final IssueFieldValueRepository issueFieldValueRepo;
	private final IssueFieldSchemaParser schemaParser;

	public List<IssueFieldValue> validateAndExtract(Map<Long, Object> inputById, Issue issue) {
		List<IssueField> issueFields = issueFieldRepo.findByIssueType(issue.getIssueType());
		List<IssueFieldValue> resultValues = new ArrayList<>(issueFields.size());

		for (IssueField field : issueFields) {
			Long fieldId = field.getId();
			Object rawInput = inputById.get(fieldId);

			ensureRequiredValuePresent(field, rawInput);

			// 값이 없으면(선택 필드) 스킵
			if (rawInput == null || isBlankString(rawInput)) {
				continue;
			}

			Object domainValue = schemaParser.toDomainValue(field, rawInput);
			resultValues.add(IssueFieldValue.of(issue, field, domainValue));
		}

		return resultValues;
	}

	public List<IssueFieldValue> validateAndApplyPartialUpdate(Map<Long, Object> inputById, Issue issue) {
		Map<Long, IssueField> fieldMap = loadFieldMap(issue);
		Map<Long, IssueFieldValue> existingValueMap = loadExistingValueMap(issue);

		List<IssueFieldValue> valuesToPersist = new ArrayList<>(inputById.size());

		for (Map.Entry<Long, Object> entry : inputById.entrySet()) {
			Long fieldId = entry.getKey();
			Object rawInput = entry.getValue();

			IssueField fieldDefinition = requireKnownField(fieldMap, fieldId);

			ensureRequiredValuePresent(fieldDefinition, rawInput);

			// null/blank → 값 삭제
			if (rawInput == null || isBlankString(rawInput)) {
				IssueFieldValue existing = existingValueMap.get(fieldId);
				if (existing != null) {
					existing.clearValue();
					valuesToPersist.add(existing);
				}
				continue;
			}

			Object domainValue = schemaParser.toDomainValue(fieldDefinition, rawInput);

			IssueFieldValue value = existingValueMap.get(fieldId);
			if (value == null) {
				value = IssueFieldValue.of(issue, fieldDefinition, domainValue);
			} else {
				value.updateValue(domainValue);
			}
			valuesToPersist.add(value);
		}

		return valuesToPersist;
	}

	/**
	 * Return if the issue field is not required;
	 * otherwise throw if the value is null or blank when it is required
	 */
	private void ensureRequiredValuePresent(IssueField field, Object rawInput) {
		if (!field.isRequired()) {
			return;
		}
		if (rawInput == null) {
			throw new InvalidCustomFieldException("Field(id: '%d') is required.".formatted(field.getId()));
		}
		if (isBlankString(rawInput)) {
			throw new InvalidCustomFieldException("Field(id: '%d') is required.".formatted(field.getId()));
		}
	}

	private Map<Long, IssueField> loadFieldMap(Issue issue) {
		return issueFieldRepo.findByIssueType(issue.getIssueType()).stream()
			.collect(Collectors.toMap(IssueField::getId, it -> it));
	}

	private Map<Long, IssueFieldValue> loadExistingValueMap(Issue issue) {
		return issueFieldValueRepo.findByIssue(issue).stream()
			.collect(Collectors.toMap(val -> val.getField().getId(), it -> it));
	}

	private IssueField requireKnownField(Map<Long, IssueField> fieldMap, Long fieldId) {
		IssueField field = fieldMap.get(fieldId);
		if (field == null) {
			throw new InvalidCustomFieldException("Unknown custom field(id: '%d')".formatted(fieldId));
		}
		return field;
	}

	private boolean isBlankString(Object rawInput) {
		if (rawInput instanceof String stringValue) {
			return stringValue.isBlank();
		}
		return false;
	}
}
