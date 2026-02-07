package com.tissue.issue.domain.service;

import com.tissue.common.dto.FieldChange;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.IssueFieldValue;
import com.tissue.issuetype.domain.EnumFieldOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

// TODO: write javadoc
// TODO: needs refactoring
@Component
public class IssueFieldChangeTracker {

    // TODO: Make "customFields." into a constant

    /**
     * 현재 이슈의 커스텀 필드 상태를 비교하기 쉬운 Map 형태(Snapshot)로 추출 Key: IssueField ID (String) Value: Formatted
     * Value (Label, Date String etc...)
     */
    public Map<String, Object> captureSnapshot(Issue issue) {
        return issue.getFieldValues().stream()
                .filter(IssueFieldValue::isValuePresent) // 값이 있는 것만
                .collect(Collectors.toMap(
                        fv -> String.valueOf(fv.getField().getId()),
                        this::formatValue,
                        (oldVal, newVal) -> newVal // 중복 발생 시 최신 값 사용
                        ));
    }

    /** 두 스냅샷(Map)을 비교하여 변경된 내역을 반환 */
    public Map<String, FieldChange> compareChanges(Map<String, Object> oldSnapshot, Map<String, Object> newSnapshot) {
        Map<String, FieldChange> changes = new HashMap<>();

        // 생성되거나 변경된 값
        for (Map.Entry<String, Object> entry : newSnapshot.entrySet()) {
            String fieldIdStr = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldSnapshot.get(fieldIdStr);

            if (!Objects.equals(oldValue, newValue)) {
                // Key를 "customFields.{ID}" 형태로 저장
                changes.put("customFields." + fieldIdStr, new FieldChange(oldValue, newValue));
            }
        }

        // 삭제된 값
        for (String fieldIdStr : oldSnapshot.keySet()) {
            if (!newSnapshot.containsKey(fieldIdStr)) {
                changes.put("customFields." + fieldIdStr, new FieldChange(oldSnapshot.get(fieldIdStr), null));
            }
        }

        return changes;
    }

    /** 내부 값을 로그/비교용 데이터로 변환 */
    private @Nullable Object formatValue(IssueFieldValue fv) {
        Object value = fv.getValue();

        if (value == null) {
            throw new IllegalStateException("Field value is missing for field '%s'(id: %d)."
                    .formatted(fv.getField().getName(), fv.getField().getId()));
        }

        // EnumOption인 경우 ID나 객체 주소 대신 Label을 저장
        if (value instanceof EnumFieldOption option) {
            return option.getName();
        }

        return value;
    }
}
