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

    // field id (string) -> field name
    private static final Map<String, String> NAMES = Map.of("1", "version", "2", "environment", "3", "severity");
    private final IssueFieldChangeTracker sut = new IssueFieldChangeTracker();

    @Nested
    @DisplayName("compare changes")
    class CompareChanges {

        @Test
        @DisplayName("success: detects created field, keyed by capitalised field name")
        void successDetectCreatedField() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>();
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "new value"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot, NAMES);

            assertThat(changes).containsExactly(entry("Version", new FieldChange(null, "new value")));
        }

        @Test
        @DisplayName("success: detects updated field")
        void successDetectUpdatedField() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "old value"));
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "new value"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot, NAMES);

            // then
            assertThat(changes).containsExactly(entry("Version", new FieldChange("old value", "new value")));
        }

        @Test
        @DisplayName("success: detects deleted field")
        void successDetectDeletedField() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "old value"));
            Map<String, Object> newSnapshot = new HashMap<>();

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot, NAMES);

            // then
            assertThat(changes).containsExactly(entry("Version", new FieldChange("old value", null)));
        }

        @Test
        @DisplayName("success: returns empty map when no changes")
        void successNoChanges() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>(Map.of("1", "same"));
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("1", "same"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot, NAMES);

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
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot, NAMES);

            // then
            assertThat(changes)
                    .containsOnly(
                            entry("Version", new FieldChange("old", "new")),
                            entry("Environment", new FieldChange("deleted", null)),
                            entry("Severity", new FieldChange(null, "created")));
        }

        @Test
        @DisplayName("success: falls back to the id when the field name is unknown")
        void successFallbackToId() {
            // given
            Map<String, Object> oldSnapshot = new HashMap<>();
            Map<String, Object> newSnapshot = new HashMap<>(Map.of("99", "value"));

            // when
            Map<String, FieldChange> changes = sut.compareChanges(oldSnapshot, newSnapshot, NAMES);

            // then
            assertThat(changes).containsExactly(entry("99", new FieldChange(null, "value")));
        }
    }
}
