package com.tissue.issue.application.service.validator;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issue.domain.service.handler.IssueFieldTypeHandlerRegistry;
import com.tissue.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.issuetype.domain.IssueField;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldSchemaValidator {

	private final IssueFieldQueryRepository issueFieldRepo;
	private final IssueFieldTypeHandlerRegistry fieldTypeHandler;

	public void validateAndAssign(Map<Long, Object> rawInputById, Issue issue) {
		List<IssueField> fields = loadFields(issue);

		for (IssueField field : fields) {
			Object raw = rawInputById.get(field.getId());
			ensureValueExistsIfRequired(field, raw);

			if (isEmptyValue(field, raw)) {
				continue;
			}

			IssueFieldValue val = issue.addOrUpdateFieldValue(field);
			parseAndAssignValue(val, field, raw);
		}
	}

	public void validateAndApplyPatch(Map<Long, Object> rawInputById, Issue issue) {
		Map<Long, IssueField> defMap = loadFieldMap(issue);

		for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
			applyOnePatchEntry(issue, defMap, e.getKey(), e.getValue());
		}
	}

	private void applyOnePatchEntry(
		Issue issue,
		Map<Long, IssueField> fieldMap,
		Long fieldId,
		Object raw
	) {
		IssueField field = requireKnownField(fieldMap, fieldId);
		ensureValueExistsIfRequired(field, raw);

		IssueFieldValue fieldValue = issue.addOrUpdateFieldValue(field);

		if (isEmptyValue(field, raw)) {
			fieldValue.clearValue();
			return;
		}

		Object parsed = fieldTypeHandler.parse(field, raw);
		fieldTypeHandler.assign(fieldValue, parsed);
	}

	private List<IssueField> loadFields(Issue issue) {
		return issueFieldRepo.findByIssueType(issue.getIssueType());
	}

	private Map<Long, IssueField> loadFieldMap(Issue issue) {
		return loadFields(issue).stream()
			.collect(Collectors.toMap(IssueField::getId, it -> it));
	}

	private boolean isEmptyValue(IssueField field, Object raw) {
		return fieldTypeHandler.isBlank(field, raw);
	}

	private void parseAndAssignValue(IssueFieldValue val, IssueField field, Object raw) {
		Object parsed = fieldTypeHandler.parse(field, raw);
		fieldTypeHandler.assign(val, parsed);
	}

	private void ensureValueExistsIfRequired(IssueField field, Object raw) {
		boolean fieldNotRequired = !field.isRequired();
		if (fieldNotRequired) {
			return;
		}
		if (isEmptyValue(field, raw)) {
			throw new BadRequestException(CUSTOM_FIELD_REQUIRED)
				.addContext(ISSUE_TYPE_ID, field.getIssueType().getId())
				.addContext(ISSUE_TYPE, field.getIssueType().getDisplayLabel())
				.addContext(ISSUE_FIELD_ID, field.getId())
				.addContext(ISSUE_FIELD, field.getDisplayLabel());
		}
	}

	private IssueField requireKnownField(Map<Long, IssueField> map, Long id) {
		IssueField field = map.get(id);
		if (field == null) {
			throw new BadRequestException(UNKNOWN_CUSTOM_FIELD_ID)
				.addContext(ISSUE_FIELD_ID, id);
		}
		return field;
	}
}
