package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.FieldChange;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ActivitySentenceTest {

    @ParameterizedTest
    @EnumSource(ActivityType.class)
    @DisplayName("success: every activity type renders a finished sentence, even with nothing to go on")
    void everyTypeRendersASentence(ActivityType type) {
        String sentence = ActivitySentence.summarize(log(type, Map.of()));

        assertThat(sentence).isNotBlank();
        assertThat(sentence).endsWith(".");
        assertThat(sentence).doesNotContain("null");
        assertThat(sentence).doesNotContain("  ");
    }

    @Nested
    @DisplayName("the publisher's placeholder values")
    class PlaceholderValues {

        @Test
        @DisplayName("success: an unset story point is not read back as the word null")
        void unsetStoryPointReadsAsBeingSet() {
            // the publisher stores String.valueOf((Integer) null), which is the text "null", not an absent key
            Map<String, String> data = Map.of("actorName", "Kim", "oldPoint", "null", "newPoint", "5");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_STORY_POINT_CHANGED, data));

            assertThat(sentence).isEqualTo("Kim set the story point to 5.");
        }

        @Test
        @DisplayName("success: clearing a story point says so instead of moving it to nothing")
        void clearedStoryPointSaysCleared() {
            Map<String, String> data = Map.of("actorName", "Kim", "oldPoint", "5", "newPoint", "null");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_STORY_POINT_CHANGED, data));

            assertThat(sentence).isEqualTo("Kim cleared the story point, which was 5.");
        }

        @Test
        @DisplayName("success: an absent parent is stored as an empty string and reads as being set")
        void addingAParentReadsAsBeingSet() {
            Map<String, String> data = Map.of("actorName", "Kim", "oldParent", "", "newParent", "PROJ-9");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_PARENT_CHANGED, data));

            assertThat(sentence).isEqualTo("Kim put the issue under PROJ-9.");
        }

        @Test
        @DisplayName("success: removing a parent names the one it left")
        void removingAParentNamesTheOldOne() {
            Map<String, String> data = Map.of("actorName", "Kim", "oldParent", "PROJ-9", "newParent", "");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_PARENT_CHANGED, data));

            assertThat(sentence).isEqualTo("Kim took the issue out from under PROJ-9.");
        }

        @Test
        @DisplayName("success: an unresolved VCS user is left out rather than named UNKNOWN")
        void unknownVcsUserIsOmitted() {
            Map<String, String> data = Map.of(
                    "oldState", "In Review",
                    "newState", "Done",
                    "triggerReason", "GITHUB PR MERGED",
                    "vcsUserName", "UNKNOWN");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM, data));

            assertThat(sentence)
                    .isEqualTo("Automation moved the issue from In Review to Done, triggered by GITHUB PR MERGED.");
        }

        @Test
        @DisplayName("success: an empty trigger reason is left out rather than trailing a dangling clause")
        void emptyTriggerReasonIsOmitted() {
            Map<String, String> data =
                    Map.of("oldState", "In Review", "newState", "Done", "triggerReason", "", "vcsUserName", "kim");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM, data));

            assertThat(sentence).isEqualTo("Automation moved the issue from In Review to Done from kim.");
        }
    }

    @Nested
    @DisplayName("the sentences a reader gets")
    class Wording {

        @Test
        @DisplayName("success: a transition names both states")
        void transitionNamesBothStates() {
            Map<String, String> data = Map.of("actorName", "Kim", "oldState", "To Do", "newState", "In Progress");

            assertThat(ActivitySentence.summarize(log(ActivityType.ISSUE_WORKFLOW_TRANSITIONED, data)))
                    .isEqualTo("Kim moved the issue from To Do to In Progress.");
        }

        @Test
        @DisplayName("success: a relation reads from this issue's side so the direction is unambiguous")
        void relationReadsFromThisIssuesSide() {
            Map<String, String> data = Map.of(
                    "sourceIssueKey", "PROJ-1",
                    "actorName", "Kim",
                    "relationType", "BLOCKS",
                    "targetIssueKey", "PROJ-2");

            assertThat(ActivitySentence.summarize(log(ActivityType.ISSUE_RELATION_ADDED, data)))
                    .isEqualTo("Kim recorded that this issue blocks PROJ-2.");
        }

        @Test
        @DisplayName("success: a verdict is stated outright rather than as an enum name")
        void reviewVerdictIsPlainEnglish() {
            assertThat(ActivitySentence.summarize(log(
                            ActivityType.ISSUE_REVIEW_SUBMITTED,
                            Map.of("actorName", "Kim", "reviewStatus", "CHANGES_REQUESTED"))))
                    .isEqualTo("Kim reviewed the issue and requested changes.");

            assertThat(ActivitySentence.summarize(log(
                            ActivityType.ISSUE_REVIEW_SUBMITTED,
                            Map.of("actorName", "Kim", "reviewStatus", "APPROVED"))))
                    .isEqualTo("Kim approved the issue.");
        }

        @Test
        @DisplayName("success: a single reviewer is not asked as 1 reviewers")
        void reviewerCountIsPluralized() {
            assertThat(ActivitySentence.summarize(
                            log(ActivityType.ISSUE_REVIEW_REQUESTED, Map.of("actorName", "Kim", "reviewerCount", "1"))))
                    .contains("1 reviewer to review");

            assertThat(ActivitySentence.summarize(
                            log(ActivityType.ISSUE_REVIEW_REQUESTED, Map.of("actorName", "Kim", "reviewerCount", "3"))))
                    .contains("3 reviewers to review");
        }

        @Test
        @DisplayName("success: a merged pull request says merged, not the enum it arrived as")
        void pullRequestActionIsPlainEnglish() {
            Map<String, String> data = Map.of("actorName", "Kim", "prAction", "MERGED", "prTitle", "TIS-1 login");

            assertThat(ActivitySentence.summarize(log(ActivityType.ISSUE_VCS_CONNECTION_LINKED, data)))
                    .isEqualTo("Pull request \"TIS-1 login\" by Kim was merged.");
        }

        @Test
        @DisplayName("success: a merge is not credited to the person the row names")
        void pullRequestDoesNotCreditTheStoredNameWithTheAction() {
            // the stored name is the PR's author; the webhook's sender - who actually merged it - is never read,
            // so an active "Kim merged it" would credit the merge to whoever wrote the branch
            Map<String, String> data = Map.of("actorName", "Kim", "prAction", "MERGED", "prTitle", "TIS-1 login");

            String sentence = ActivitySentence.summarize(log(ActivityType.ISSUE_VCS_CONNECTION_LINKED, data));

            assertThat(sentence).doesNotContain("Kim merged");
            assertThat(sentence).contains("was merged");
        }

        @Test
        @DisplayName("success: a pull request with no title still reads as a sentence")
        void untitledPullRequestStillReads() {
            Map<String, String> data = Map.of("actorName", "Kim", "prAction", "OPENED", "prTitle", "");

            assertThat(ActivitySentence.summarize(log(ActivityType.ISSUE_VCS_CONNECTION_LINKED, data)))
                    .isEqualTo("A pull request by Kim was opened.");
        }

        @Test
        @DisplayName("success: a re-review that reset nothing does not claim reviewers were asked")
        void reReviewThatResetNothingSaysSo() {
            // the count is how many verdicts were reset, so naming three still-pending reviewers records zero
            Map<String, String> data = Map.of("actorName", "Kim", "reviewerCount", "0");

            assertThat(ActivitySentence.summarize(log(ActivityType.ISSUE_REVIEW_REQUESTED, data)))
                    .isEqualTo("Kim requested a re-review, but no reviewer had given a verdict to reset.");
        }

        @Test
        @DisplayName("success: a row with no actor still reads as a sentence")
        void missingActorFallsBackToSomeone() {
            assertThat(ActivitySentence.summarize(log(ActivityType.ISSUE_CREATED, Map.of())))
                    .isEqualTo("Someone created the issue.");
        }
    }

    @Nested
    @DisplayName("the change lines under a sentence")
    class Changes {

        @Test
        @DisplayName("success: a field edit lists what moved, in a stable order")
        void fieldEditListsWhatMoved() {
            Map<String, FieldChange> changes = new HashMap<>();
            changes.put("priority", new FieldChange("P2", "P0"));
            changes.put("dueAt", new FieldChange(null, "2026-09-01"));
            changes.put("summary", new FieldChange("old", null));

            assertThat(ActivitySentence.changeLines(log(ActivityType.ISSUE_UPDATED, Map.of(), changes)))
                    .containsExactly("dueAt: 2026-09-01", "priority: P2 -> P0", "summary: old (cleared)");
        }

        @Test
        @DisplayName("success: a body edit reports that it changed rather than printing both documents")
        void contentChangeIsNotPrinted() {
            Map<String, FieldChange> changes =
                    Map.of("content", new FieldChange("a very long body", "another very long body"));

            assertThat(ActivitySentence.changeLines(log(ActivityType.ISSUE_UPDATED, Map.of(), changes)))
                    .containsExactly("content updated");
        }

        @Test
        @DisplayName("success: a diff the sentence already states is not repeated underneath")
        void diffAlreadyInTheSentenceIsSuppressed() {
            Map<String, FieldChange> changes = Map.of("state", new FieldChange("To Do", "Done"));

            assertThat(ActivitySentence.changeLines(log(ActivityType.ISSUE_WORKFLOW_TRANSITIONED, Map.of(), changes)))
                    .isEmpty();
        }

        @Test
        @DisplayName("success: a change with neither a before nor an after is dropped")
        void emptyDiffIsDropped() {
            Map<String, FieldChange> changes = new HashMap<>();
            changes.put("summary", new FieldChange(null, null));

            assertThat(ActivitySentence.changeLines(log(ActivityType.ISSUE_UPDATED, Map.of(), changes)))
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("success: no sentence leaks a raw data key at a reader")
    void noSentenceLeaksARawKey() {
        Map<String, String> everyKey = new HashMap<>();
        for (String key : Arrays.asList(
                "projectKey",
                "issueKey",
                "actorName",
                "assigneeName",
                "removedAssigneeName",
                "reviewerName",
                "removedReviewerName",
                "reviewStatus",
                "reviewerCount",
                "oldState",
                "newState",
                "oldPoint",
                "newPoint",
                "oldParent",
                "newParent",
                "sourceIssueKey",
                "relationType",
                "targetIssueKey",
                "sprintTitle",
                "vcsProvider",
                "branchName",
                "repoUrl",
                "prTitle",
                "prUrl",
                "prAction",
                "vcsUserEmail",
                "vcsUserName",
                "triggerReason")) {
            everyKey.put(key, "X");
        }

        for (ActivityType type : ActivityType.values()) {
            String sentence = ActivitySentence.summarize(log(type, everyKey));

            assertThat(sentence).as("%s", type).doesNotContain("{");
            assertThat(sentence).as("%s", type).doesNotContain("actorName");
            assertThat(sentence).as("%s", type).doesNotContain("issueKey");
        }
    }

    private static ActivityLogResponse log(ActivityType type, Map<String, String> data) {
        return log(type, data, Map.of());
    }

    private static ActivityLogResponse log(
            ActivityType type, Map<String, String> data, Map<String, FieldChange> changes) {
        return ActivityLogResponse.builder()
                .id(1L)
                .eventId(UUID.nameUUIDFromBytes(new byte[] {1}))
                .type(type)
                .data(data)
                .changes(changes)
                .actorMemberId(7L)
                .occurredAt(Instant.EPOCH)
                .build();
    }
}
