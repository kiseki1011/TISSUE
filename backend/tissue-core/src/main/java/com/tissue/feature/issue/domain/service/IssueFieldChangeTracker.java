package com.tissue.feature.issue.domain.service;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issue.domain.service.handler.IssueFieldTypeHandlerRegistry;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.shared.dto.FieldChange;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Tracks changes in issue custom fields by comparing snapshots of field values.
 * Used to generate detailed change logs for activity logs and notifications.
 */
@Component
@RequiredArgsConstructor
public class IssueFieldChangeTracker {

    private static final String CUSTOM_FIELD_PREFIX = "customFields.";

    private final IssueFieldTypeHandlerRegistry handlerRegistry;

    /**
     * Captures the current state of custom fields as a snapshot map.
     *
     * @param issue The issue to capture
     * @return A map where keys are {@link IssueField} IDs and values are formatted domain values
     */
    public Map<String, Object> captureSnapshot(Issue issue) {
        return issue.getFieldValues().stream()
                .filter(IssueFieldValue::isValuePresent)
                // spotless:off
                .collect(Collectors.toMap(
                    fv -> String.valueOf(fv.getField().getId()),
                    this::formatValue,
                    (oldVal, newVal) -> newVal));
    }           // spotless:on

    /**
     * Compares two snapshots and returns a map of changed fields.
     */
    public Map<String, FieldChange> compareChanges(Map<String, Object> oldSnapshot, Map<String, Object> newSnapshot) {
        Map<String, FieldChange> changes = new HashMap<>();

        // created/updated values
        for (Map.Entry<String, Object> entry : newSnapshot.entrySet()) {
            String fieldIdStr = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldSnapshot.get(fieldIdStr);

            if (!Objects.equals(oldValue, newValue)) {
                changes.put(CUSTOM_FIELD_PREFIX + fieldIdStr, new FieldChange(oldValue, newValue));
            }
        }

        // deleted values
        for (String fieldIdStr : oldSnapshot.keySet()) {
            if (!newSnapshot.containsKey(fieldIdStr)) {
                changes.put(CUSTOM_FIELD_PREFIX + fieldIdStr, new FieldChange(oldSnapshot.get(fieldIdStr), null));
            }
        }

        return changes;
    }

    /**
     * Retrieves the typed value from the registry and formats it for comparison/logging.
     */
    private @Nullable Object formatValue(IssueFieldValue fv) {
        Object value = handlerRegistry.getValue(fv);

        if (value == null) {
            throw new IllegalStateException("Field value is missing for field '%s'(id: %d)."
                    .formatted(fv.getField().getName(), fv.getField().getId()));
        }
        if (value instanceof FieldOption option) {
            return option.getName();
        }

        return value;
    }
}
