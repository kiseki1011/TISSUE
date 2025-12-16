package com.tissue.issue.application.service.validator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.issue.application.port.out.IssueFieldValueQueryRepository;
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
	private final IssueFieldValueQueryRepository issueFieldValueRepo;
	private final IssueFieldTypeHandlerRegistry fieldTypeHandler;

	// public List<IssueFieldValue> validateAndExtract(Map<Long, Object> rawInputById, Issue issue) {
	// 	List<IssueField> fields = loadFields(issue);
	// 	List<IssueFieldValue> issueFieldValues = new ArrayList<>(fields.size());
	//
	// 	for (IssueField field : fields) {
	// 		Object raw = rawInputById.get(field.getId());
	//
	// 		requireValueIfRequired(field, raw);
	//
	// 		IssueFieldValue val = IssueFieldValue.of(issue, field);
	//
	// 		if (isEmptyValue(field, raw)) {
	// 			val.clearValue();
	// 			issueFieldValues.add(val);
	// 			continue;
	// 		}
	//
	// 		issueFieldValues.add(parseAndAssignValue(val, field, raw));
	// 	}
	// 	return issueFieldValues;
	// }

	// public List<IssueFieldValue> validateAndApplyPatch(Map<Long, Object> rawInputById, Issue issue) {
	// 	Map<Long, IssueField> defMap = loadFieldMap(issue);
	// 	Map<Long, IssueFieldValue> existing = loadExistingValueMap(issue);
	//
	// 	List<IssueFieldValue> toUpdate = new ArrayList<>(rawInputById.size());
	//
	// 	for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
	// 		applyOnePatchEntry(issue, defMap, existing, e.getKey(), e.getValue())
	// 			.ifPresent(toUpdate::add);
	// 	}
	// 	return toUpdate;
	// }

	// Create 용 - Issue가 아직 저장이 안 된 상태일 수 있으므로 로직 유지하되 반환값 활용
	public void validateAndAssign(Map<Long, Object> rawInputById, Issue issue) {
		List<IssueField> fields = loadFields(issue);

		for (IssueField field : fields) {
			Object raw = rawInputById.get(field.getId());
			requireValueIfRequired(field, raw);

			// Issue의 컬렉션에 추가 (빈 값이면 스킵하거나 clear)
			if (isEmptyValue(field, raw)) {
				// Create 시점엔 값이 없으면 굳이 만들 필요 없음 (혹은 빈 값으로 만듦)
				continue;
			}

			// Issue 내부 컬렉션에 추가됨
			IssueFieldValue val = issue.addOrUpdateFieldValue(field);
			parseAndAssignValue(val, field, raw);
		}
	}

	// Update 용 - Issue의 컬렉션을 직접 수정
	public void validateAndApplyPatch(Map<Long, Object> rawInputById, Issue issue) {
		Map<Long, IssueField> defMap = loadFieldMap(issue);

		// 리포지토리 조회가 아니라, Issue 객체의 메모리 상 컬렉션을 가져옴 (필요 시 Lazy Loading 발동)
		Map<Long, IssueFieldValue> existingMap = issue.getFieldValues().stream()
			.collect(Collectors.toMap(v -> v.getField().getId(), v -> v));

		for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
			applyOnePatchEntry(issue, defMap, existingMap, e.getKey(), e.getValue());
		}

		// 반환값 없음 (Issue 객체 내부 상태가 변했으므로)
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
			// TODO: InvalidCustomFieldException vs IllegalStateException
			throw new RuntimeException("Field(id:%d) is required".formatted(field.getId()));
		}
	}

	private boolean isEmptyValue(IssueField field, Object raw) {
		return fieldTypeHandler.isBlank(field, raw);
	}

	// 파싱 후 신규 값 엔티티 생성 및 칼럼에 할당
	private void parseAndAssignValue(IssueFieldValue val, IssueField field, Object raw) {
		Object parsed = fieldTypeHandler.parse(field, raw);
		fieldTypeHandler.assign(val, parsed);
	}

	// private Optional<IssueFieldValue> applyOnePatchEntry(
	// 	Issue issue,
	// 	Map<Long, IssueField> fieldMap,
	// 	Map<Long, IssueFieldValue> existing,
	// 	Long fieldId,
	// 	Object raw
	// ) {
	// 	IssueField field = requireKnown(fieldMap, fieldId);
	//
	// 	requireValueIfRequired(field, raw);
	//
	// 	IssueFieldValue fieldValue = getFieldValue(existing, fieldId, issue, field);
	//
	// 	if (isEmptyValue(field, raw)) {
	// 		fieldValue.clearValue();
	// 		return Optional.of(fieldValue);
	// 	}
	//
	// 	Object parsed = fieldTypeHandler.parse(field, raw);
	// 	fieldTypeHandler.assign(fieldValue, parsed);
	// 	return Optional.of(fieldValue);
	// }

	private void applyOnePatchEntry(
		Issue issue,
		Map<Long, IssueField> fieldMap,
		Map<Long, IssueFieldValue> existing, // 이제 필요 없음, issue.addOrUpdateFieldValue 사용
		Long fieldId,
		Object raw
	) {
		IssueField field = requireKnown(fieldMap, fieldId);
		requireValueIfRequired(field, raw);

		// Issue 엔티티에게 객체를 달라고 요청 (없으면 만들어서 줌)
		IssueFieldValue fieldValue = issue.addOrUpdateFieldValue(field);

		if (isEmptyValue(field, raw)) {
			fieldValue.clearValue();
			// 만약 값을 아예 지우고 싶다면(Row 삭제) issue.removeFieldValue(fieldValue) 로직 필요
			// 현재는 clearValue()로 값만 비우는 정책으로 유지
			return;
		}

		Object parsed = fieldTypeHandler.parse(field, raw);
		fieldTypeHandler.assign(fieldValue, parsed);
	}

	private IssueField requireKnown(Map<Long, IssueField> map, Long id) {
		IssueField field = map.get(id);
		if (field == null) {
			// TODO: InvalidCustomFieldException vs IllegalStateException vs IllegalArgumentException
			throw new IllegalArgumentException("Unknown custom field(id:%d)".formatted(id));
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
