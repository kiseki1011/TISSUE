package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.service.ProjectQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectLastActivityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ActivityLogCommandRepository activityLogCommandRepository;

    private Member gildong;

    @BeforeEach
    void setUp() {
        gildong = memberRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("returns null lastActivity when the project has no activity")
    void nullWhenNoActivity() {
        // given
        createProject("APPLE");

        // when
        Instant lastActivityAt = summaryOf("APPLE").lastActivityAt();

        // then
        assertThat(lastActivityAt).isNull();
    }

    @Test
    @DisplayName("reflects an issue activity in lastActivity")
    void reflectsIssueActivity() {
        // given
        createProject("APPLE");
        ActivityLog activity = saveActivity(ActivityType.ISSUE_CREATED, EntityReference.forIssue("APPLE", "APPLE-1"));

        // when
        Instant lastActivityAt = summaryOf("APPLE").lastActivityAt();

        // then
        assertThat(lastActivityAt).isEqualTo(activity.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    @DisplayName("counts comment activity toward lastActivity")
    void countsCommentActivity() {
        // given
        createProject("APPLE");
        saveActivity(ActivityType.ISSUE_COMMENT_ADDED, EntityReference.forIssueComment("APPLE", "APPLE-1", 1L));

        // when
        Instant lastActivityAt = summaryOf("APPLE").lastActivityAt();

        // then
        assertThat(lastActivityAt).isNotNull();
    }

    @Test
    @DisplayName("ignores sprint activity for lastActivity")
    void ignoresSprintActivity() {
        // given
        createProject("APPLE");
        saveActivity(ActivityType.SPRINT_STARTED, EntityReference.forSprint("APPLE", 1L));

        // when
        Instant lastActivityAt = summaryOf("APPLE").lastActivityAt();

        // then
        assertThat(lastActivityAt).isNull();
    }

    @Test
    @DisplayName("returns the most recent issue activity as lastActivity")
    void reflectsLatestActivity() {
        // given
        createProject("APPLE");
        ActivityLog first = saveActivity(ActivityType.ISSUE_CREATED, EntityReference.forIssue("APPLE", "APPLE-1"));
        ActivityLog second = saveActivity(ActivityType.ISSUE_UPDATED, EntityReference.forIssue("APPLE", "APPLE-1"));

        // when
        Instant lastActivityAt = summaryOf("APPLE").lastActivityAt();

        // then
        Instant firstAt = first.getCreatedAt().truncatedTo(ChronoUnit.MICROS);
        Instant secondAt = second.getCreatedAt().truncatedTo(ChronoUnit.MICROS);
        assertThat(lastActivityAt).isEqualTo(firstAt.isAfter(secondAt) ? firstAt : secondAt);
    }

    @Test
    @DisplayName("scopes lastActivity per project")
    void scopedPerProject() {
        // given
        createProject("APPLE");
        createProject("BANANA");
        saveActivity(ActivityType.ISSUE_CREATED, EntityReference.forIssue("APPLE", "APPLE-1"));

        // when // then
        assertThat(summaryOf("APPLE").lastActivityAt()).isNotNull();
        assertThat(summaryOf("BANANA").lastActivityAt()).isNull();
    }

    private void createProject(String key) {
        projectRepository.save(Project.create(key, key, null));
        em.flush();
        em.clear();
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

    private ProjectSummary summaryOf(String key) {
        Page<ProjectSummary> page = sut.getProjects(false, null, PageRequest.of(0, 50), gildong.getId());
        return page.getContent().stream()
                .filter(summary -> summary.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
