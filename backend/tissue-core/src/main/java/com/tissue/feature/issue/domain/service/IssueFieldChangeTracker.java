package com.tissue.feature.issue.domain.service;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.shared.dto.FieldChange;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Tracks changes in issue custom fields by comparing snapshots of field values.
 * Used to generate detailed change logs for activity logs and notifications.
 */
@Component
public class IssueFieldChangeTracker {

    private static final String CUSTOM_FIELD_PREFIX = "customFields.";

    /**
     * Captures the current state of custom fields as a snapshot map.
     *
     * @param issue The issue to capture
     * @return A defensive copy of the issue's custom fields map
     */
    public Map<String, Object> captureSnapshot(Issue issue) {
        return new HashMap<>(issue.getCustomFields());
    }

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
}
