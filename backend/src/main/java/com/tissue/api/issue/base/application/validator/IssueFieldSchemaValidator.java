package com.tissue.api.issue.base.application.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	private final IssueFieldValueCodec codec; // 라우팅 + blank 규칙 + 파싱 + 할당

	public List<IssueFieldValue> validateAndExtract(Map<Long, Object> rawInputByFieldId, Issue issue) {
		List<IssueField> fieldDefs = loadFieldDefinitions(issue);
		List<IssueFieldValue> out = new ArrayList<>(fieldDefs.size());

		for (IssueField field : fieldDefs) {
			Object raw = rawInputByFieldId.get(field.getId());

			// 필수값 검사: null 또는 타입별 blank 금지
			ensureRequiredOrThrow(field, raw);

			// 선택 필드에서 빈 값이면 스킵 (DB에 아무것도 저장하지 않음)
			if (isEmptyFor(field, raw)) {
				continue;
			}

			// 파싱 + 엔티티 생성 + 값 할당
			IssueFieldValue val = parseAndCreateValue(issue, field, raw);
			out.add(val);
		}
		return out;
	}

	public List<IssueFieldValue> validateAndApplyPartialUpdate(Map<Long, Object> rawInputByFieldId, Issue issue) {
		Map<Long, IssueField> defMap = loadFieldMap(issue);
		Map<Long, IssueFieldValue> existing = loadExistingValueMap(issue);

		List<IssueFieldValue> toPersist = new ArrayList<>(rawInputByFieldId.size());

		for (Map.Entry<Long, Object> e : rawInputByFieldId.entrySet()) {
			applyOnePatchEntry(issue, defMap, existing, e.getKey(), e.getValue())
				.ifPresent(toPersist::add);
		}
		return toPersist;
	}

	// ---------- Helpers ----------
	private List<IssueField> loadFieldDefinitions(Issue issue) {
		return issueFieldRepo.findByIssueType(issue.getIssueType());
	}

	private Map<Long, IssueField> loadFieldMap(Issue issue) {
		return loadFieldDefinitions(issue).stream().collect(Collectors.toMap(IssueField::getId, it -> it));
	}

	private Map<Long, IssueFieldValue> loadExistingValueMap(Issue issue) {
		return issueFieldValueRepo.findByIssue(issue).stream()
			.collect(Collectors.toMap(v -> v.getField().getId(), v -> v));
	}

	/** required면 null/blank 금지. optional이면 통과 */
	private void ensureRequiredOrThrow(IssueField def, Object raw) {
		if (!def.isRequired()) {
			return;
		}
		if (raw == null || codec.isBlank(def, raw)) {
			throw new InvalidCustomFieldException("Field(id:%d) is required".formatted(def.getId()));
		}
	}

	/** 타입별 blank 기준 적용(null 포함) */
	private boolean isEmptyFor(IssueField def, Object raw) {
		return raw == null || codec.isBlank(def, raw);
	}

	/** 파싱 후 신규 값 엔티티 생성 및 칼럼에 할당 */
	private IssueFieldValue parseAndCreateValue(Issue issue, IssueField def, Object raw) {
		Object parsed = codec.parse(def, raw);
		IssueFieldValue val = IssueFieldValue.of(issue, def);
		codec.assign(val, parsed);
		return val;
	}

	/** 부분 업데이트 1건 처리: Optional 반환(저장할 게 없으면 빈) */
	private Optional<IssueFieldValue> applyOnePatchEntry(
		Issue issue,
		Map<Long, IssueField> defMap,
		Map<Long, IssueFieldValue> existing,
		Long fieldId,
		Object raw
	) {
		IssueField def = requireKnown(defMap, fieldId);

		// 1) 필수값 검사
		ensureRequiredOrThrow(def, raw);

		// 2) null/blank → 기존 값이 있으면 삭제(컬럼 전부 clear)
		if (isEmptyFor(def, raw)) {
			IssueFieldValue cur = existing.get(fieldId);
			if (cur != null) {
				cur.clearValue();
				return Optional.of(cur);
			}
			return Optional.empty();
		}

		// 3) 파싱 + 기존 값 갱신 or 신규 생성
		Object parsed = codec.parse(def, raw);
		IssueFieldValue cur = existing.get(fieldId);
		if (cur == null) {
			cur = IssueFieldValue.of(issue, def);
		}
		codec.assign(cur, parsed);
		return Optional.of(cur);
	}

	private IssueField requireKnown(Map<Long, IssueField> map, Long id) {
		IssueField field = map.get(id);
		if (field == null) {
			throw new InvalidCustomFieldException("Unknown custom field(id:%d)".formatted(id));
		}
		return field;
	}
}
