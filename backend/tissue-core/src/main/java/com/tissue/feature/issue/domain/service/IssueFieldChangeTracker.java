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
     * Compares two snapshots and returns a map of changed fields, keyed by the field's name (so the
     * activity log reads as the field.
     * For example, "Version", rather than "customFields.4".
     *
     * @param fieldIdToName field id (as string) -> field name, from the issue type definitions
     */
    public Map<String, FieldChange> compareChanges(
            Map<String, Object> oldSnapshot, Map<String, Object> newSnapshot, Map<String, String> fieldIdToName) {
        Map<String, FieldChange> changes = new HashMap<>();

        // created/updated values
        for (Map.Entry<String, Object> entry : newSnapshot.entrySet()) {
            String fieldIdStr = entry.getKey();
            Object newValue = entry.getValue();
            Object oldValue = oldSnapshot.get(fieldIdStr);

            if (!Objects.equals(oldValue, newValue)) {
                changes.put(fieldLabel(fieldIdStr, fieldIdToName), new FieldChange(oldValue, newValue));
            }
        }

        // deleted values
        for (String fieldIdStr : oldSnapshot.keySet()) {
            if (!newSnapshot.containsKey(fieldIdStr)) {
                changes.put(fieldLabel(fieldIdStr, fieldIdToName), new FieldChange(oldSnapshot.get(fieldIdStr), null));
            }
        }

        return changes;
    }

    /**
     * The change-log key for a custom field.
     *
     * <p>Its name with the first letter capitalized. For example, "version" -> "Version",
     * matching how the field is labeled in the issue detail. Falls back to the id when the name is
     * unknown.
     */
    private String fieldLabel(String fieldIdStr, Map<String, String> fieldIdToName) {
        String name = fieldIdToName.getOrDefault(fieldIdStr, fieldIdStr);
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
