package com.tissue.api.issue.application.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issue.domain.model.IssueFieldValue;
import com.tissue.api.issuetype.repository.IssueFieldRepository;
import com.tissue.api.issue.infrastructure.repository.IssueFieldValueRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldSchemaValidator {

	private final IssueFieldRepository issueFieldRepo;
	private final IssueFieldValueRepository issueFieldValueRepo;
	private final IssueFieldTypeHandlerRegistry fieldTypeHandler;

	public List<IssueFieldValue> validateAndExtract(Map<Long, Object> rawInputById, Issue issue) {
		List<IssueField> fields = loadFields(issue);
		List<IssueFieldValue> issueFieldValues = new ArrayList<>(fields.size());

		for (IssueField field : fields) {
			Object raw = rawInputById.get(field.getId());

			requireValueIfRequired(field, raw);

			IssueFieldValue val = IssueFieldValue.of(issue, field);

			if (isEmptyValue(field, raw)) {
				val.clearValue();
				issueFieldValues.add(val);
				continue;
			}

			issueFieldValues.add(parseAndAssignValue(val, field, raw));
		}
		return issueFieldValues;
	}

	public List<IssueFieldValue> validateAndApplyPartialUpdate(Map<Long, Object> rawInputById, Issue issue) {
		Map<Long, IssueField> defMap = loadFieldMap(issue);
		Map<Long, IssueFieldValue> existing = loadExistingValueMap(issue);

		List<IssueFieldValue> toUpdate = new ArrayList<>(rawInputById.size());

		for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
			applyOnePatchEntry(issue, defMap, existing, e.getKey(), e.getValue())
				.ifPresent(toUpdate::add);
		}
		return toUpdate;
	}

	private List<IssueField> loadFields(Issue issue) {
		return issueFieldRepo.findByIssueType(issue.getIssueType());
	}

	private Map<Long, IssueField> loadFieldMap(Issue issue) {
		return loadFields(issue).stream().collect(Collectors.toMap(IssueField::getId, it -> it));
	}

	private Map<Long, IssueFieldValue> loadExistingValueMap(Issue issue) {
		return issueFieldValueRepo.findByIssue(issue).stream()
			.collect(Collectors.toMap(v -> v.getField().getId(), v -> v));
	}

	// required면 null/blank 금지
	private void requireValueIfRequired(IssueField field, Object raw) {
		boolean fieldNotRequired = !field.isRequired();
		if (fieldNotRequired) {
			return;
		}
		if (isEmptyValue(field, raw)) {
			throw new InvalidCustomFieldException("Field(id:%d) is required".formatted(field.getId()));
		}
	}

	private boolean isEmptyValue(IssueField field, Object raw) {
		return fieldTypeHandler.isBlank(field, raw);
	}

	/** 파싱 후 신규 값 엔티티 생성 및 칼럼에 할당 */
	private IssueFieldValue parseAndAssignValue(IssueFieldValue val, IssueField field, Object raw) {
		Object parsed = fieldTypeHandler.parse(field, raw);
		fieldTypeHandler.assign(val, parsed);
		return val;
	}
	
	private Optional<IssueFieldValue> applyOnePatchEntry(
		Issue issue,
		Map<Long, IssueField> fieldMap,
		Map<Long, IssueFieldValue> existing,
		Long fieldId,
		Object raw
	) {
		IssueField field = requireKnown(fieldMap, fieldId);

		requireValueIfRequired(field, raw);

		IssueFieldValue fieldValue = getFieldValue(existing, fieldId, issue, field);

		if (isEmptyValue(field, raw)) {
			fieldValue.clearValue();
			return Optional.of(fieldValue);
		}

		Object parsed = fieldTypeHandler.parse(field, raw);
		fieldTypeHandler.assign(fieldValue, parsed);
		return Optional.of(fieldValue);
	}

	private IssueField requireKnown(Map<Long, IssueField> map, Long id) {
		IssueField field = map.get(id);
		if (field == null) {
			throw new InvalidCustomFieldException("Unknown custom field(id:%d)".formatted(id));
		}
		return field;
	}

	private IssueFieldValue getFieldValue(
		Map<Long, IssueFieldValue> existing,
		Long fieldId,
		Issue issue,
		IssueField field
	) {
		return existing.computeIfAbsent(fieldId, id -> IssueFieldValue.of(issue, field));
	}
}
