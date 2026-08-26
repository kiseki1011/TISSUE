package com.tissue.feature.activitylog;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-issue latest-activity aggregate behind {@code IssueSummary.lastActivityAt}. Mirrors the project
 * variant ({@code ProjectLastActivityIntegrationTest}) but keyed by issue key. Seeds {@code activity_log}
 * directly, since issue-lifecycle activity logging is not visible inside the test transaction.
 */
@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.NOT_REVIEWED,
        evaluationReason = "Needs review.",
        model = "claude-opus-4-8")
@Transactional
class IssueLastActivityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ActivityLogQueryRepository sut;

    @Autowired
    private ActivityLogCommandRepository activityLogCommandRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    private Member gildong;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("an issue key with no activity is absent from the map")
    void absentWhenNoActivity() {
        // when
        Map<String, Instant> map = sut.findLastActivityAtByIssueKeys(List.of("APPLE-1"));

        // then
        assertThat(map).doesNotContainKey("APPLE-1");
    }

    @Test
    @DisplayName("reflects an issue activity as the issue's lastActivity")
    void reflectsIssueActivity() {
        // given
        ActivityLog activity = saveActivity(ActivityType.ISSUE_CREATED, EntityReference.forIssue("APPLE", "APPLE-1"));

        // when
        Instant lastActivityAt =
                sut.findLastActivityAtByIssueKeys(List.of("APPLE-1")).get("APPLE-1");

        // then
        assertThat(lastActivityAt).isEqualTo(activity.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("counts comment activity toward the issue's lastActivity")
    void countsCommentActivity() {
        // given
        saveActivity(ActivityType.ISSUE_COMMENT_ADDED, EntityReference.forIssueComment("APPLE", "APPLE-1", 1L));

        // when
        Map<String, Instant> map = sut.findLastActivityAtByIssueKeys(List.of("APPLE-1"));

        // then
        assertThat(map.get("APPLE-1")).isNotNull();
    }

    @Test
    @DisplayName("a sprint event carries no issue key, so it never surfaces under an issue")
    void ignoresSprintActivity() {
        // given
        saveActivity(ActivityType.SPRINT_STARTED, EntityReference.forSprint("APPLE", 1L));

        // when
        Map<String, Instant> map = sut.findLastActivityAtByIssueKeys(List.of("APPLE-1"));

        // then
        assertThat(map).isEmpty();
    }

    @Test
    @DisplayName("returns the most recent activity as the issue's lastActivity")
    void reflectsLatestActivity() {
        // given
        ActivityLog first = saveActivity(ActivityType.ISSUE_CREATED, EntityReference.forIssue("APPLE", "APPLE-1"));
        ActivityLog second = saveActivity(ActivityType.ISSUE_UPDATED, EntityReference.forIssue("APPLE", "APPLE-1"));

        // when
        Instant lastActivityAt =
                sut.findLastActivityAtByIssueKeys(List.of("APPLE-1")).get("APPLE-1");

        // then
        Instant firstAt = first.getCreatedAt().truncatedTo(ChronoUnit.MICROS);
        Instant secondAt = second.getCreatedAt().truncatedTo(ChronoUnit.MICROS);
        assertThat(lastActivityAt).isEqualTo(firstAt.isAfter(secondAt) ? firstAt : secondAt);
    }

    @Test
    @DisplayName("scopes lastActivity per issue key")
    void scopedPerIssue() {
        // given
        saveActivity(ActivityType.ISSUE_CREATED, EntityReference.forIssue("APPLE", "APPLE-1"));

        // when
        Map<String, Instant> map = sut.findLastActivityAtByIssueKeys(List.of("APPLE-1", "APPLE-2"));

        // then
        assertThat(map).containsKey("APPLE-1");
        assertThat(map).doesNotContainKey("APPLE-2");
    }

    @Test
    @DisplayName("an empty key set returns an empty map")
    void emptyKeysReturnsEmpty() {
        assertThat(sut.findLastActivityAtByIssueKeys(List.of())).isEmpty();
    }

    private ActivityLog saveActivity(ActivityType type, EntityReference reference) {
        ActivityLog log = ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(type)
                .entityReference(reference)
                .actorMemberId(gildong.getId())
                .data(Map.of())
                .build();
        ActivityLog saved = activityLogCommandRepository.save(log);
        em.flush();
        return saved;
    }
}
