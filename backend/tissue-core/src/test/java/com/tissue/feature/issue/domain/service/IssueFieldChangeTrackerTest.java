package com.tissue.feature.issue.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.tissue.shared.dto.FieldChange;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueFieldChangeTrackerTest {

    public static final String CUSTOM_FIELDS = "customFields.";
    private final IssueFieldChangeTracker sut = new IssueFieldChangeTracker();

    @Nested
    @DisplayName("compare changes")
    class CompareChanges {

        @Test
        @DisplayName("success: detects created field")
        void successDetectCreatedField() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>();
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "new value"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot);

            assertThat(changes).containsExactly(entry(CUSTOM_FIELDS + "1", new FieldChange(null, "new value")));
        }

        @Test
        @DisplayName("success: detects updated field")
        void successDetectUpdatedField() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "old value"));
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "new value"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot);

            // then
            assertThat(changes).containsExactly(entry(CUSTOM_FIELDS + "1", new FieldChange("old value", "new value")));
        }

        @Test
        @DisplayName("success: detects deleted field")
        void successDetectDeletedField() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "old value"));
            Map<String, Object> newSnapshot = new HashMap<>();

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot);

            // then
            assertThat(changes).containsExactly(entry(CUSTOM_FIELDS + "1", new FieldChange("old value", null)));
        }

        @Test
        @DisplayName("success: returns empty map when no changes")
        void successNoChanges() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "same"));
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "same"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot);

            // then
            assertThat(changes).isEmpty();
        }

        @Test
        @DisplayName("success: detects multiple changes")
        void successDetectMultipleChanges() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "old", "2", "deleted"));
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "new", "3", "created"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot);

            // then
            assertThat(changes)
                    .containsOnly(
                            entry(CUSTOM_FIELDS + "1", new FieldChange("old", "new")),
                            entry(CUSTOM_FIELDS + "2", new FieldChange("deleted", null)),
                            entry(CUSTOM_FIELDS + "3", new FieldChange(null, "created")));
        }
    }
}
