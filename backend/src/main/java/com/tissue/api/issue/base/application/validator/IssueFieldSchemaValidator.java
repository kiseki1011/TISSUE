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

	public List<IssueFieldValue> validateAndExtract(Map<String, Object> inputByKey, Issue issue) {
		List<IssueField> fieldDefinitions = issueFieldRepo.findByIssueType(issue.getIssueType());
		List<IssueFieldValue> resultValues = new ArrayList<>(fieldDefinitions.size());

		for (IssueField fieldDefinition : fieldDefinitions) {
			String fieldKey = fieldDefinition.getKey();
			Object rawInput = inputByKey.get(fieldKey);

			ensureRequiredValuePresent(fieldDefinition, rawInput);

			// 값이 없으면(선택 필드) 스킵
			if (rawInput == null || isBlankString(rawInput)) {
				continue;
			}

			Object domainValue = schemaParser.toDomainValue(fieldDefinition, rawInput);
			resultValues.add(IssueFieldValue.of(issue, fieldDefinition, domainValue));
		}

		return resultValues;
	}

	public List<IssueFieldValue> validateAndApplyPartialUpdate(Map<String, Object> inputByKey, Issue issue) {
		Map<String, IssueField> fieldMap = loadFieldMap(issue);
		Map<String, IssueFieldValue> existingValueMap = loadExistingValueMap(issue);

		List<IssueFieldValue> valuesToPersist = new ArrayList<>(inputByKey.size());

		for (Map.Entry<String, Object> entry : inputByKey.entrySet()) {
			String fieldKey = entry.getKey();
			Object rawInput = entry.getValue();

			IssueField fieldDefinition = requireKnownField(fieldMap, fieldKey);

			ensureRequiredValuePresent(fieldDefinition, rawInput);

			// null/blank → 값 삭제
			if (rawInput == null || isBlankString(rawInput)) {
				IssueFieldValue existing = existingValueMap.get(fieldKey);
				if (existing != null) {
					existing.clearValue();
					valuesToPersist.add(existing);
				}
				continue;
			}

			Object domainValue = schemaParser.toDomainValue(fieldDefinition, rawInput);

			IssueFieldValue value = existingValueMap.get(fieldKey);
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
			throw new InvalidCustomFieldException("Field '%s' is required.".formatted(field.getKey()));
		}
		if (isBlankString(rawInput)) {
			throw new InvalidCustomFieldException("Field '%s' is required.".formatted(field.getKey()));
		}
	}

	private Map<String, IssueField> loadFieldMap(Issue issue) {
		return issueFieldRepo.findByIssueType(issue.getIssueType()).stream()
			.collect(Collectors.toMap(IssueField::getKey, it -> it));
	}

	private Map<String, IssueFieldValue> loadExistingValueMap(Issue issue) {
		return issueFieldValueRepo.findByIssue(issue).stream()
			.collect(Collectors.toMap(val -> val.getField().getKey(), it -> it));
	}

	private IssueField requireKnownField(Map<String, IssueField> fieldMap, String fieldKey) {
		IssueField field = fieldMap.get(fieldKey);
		if (field == null) {
			throw new InvalidCustomFieldException("Unknown custom field: '%s'".formatted(fieldKey));
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
