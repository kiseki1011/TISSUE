package com.tissue.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.admin.application.service.AdminActivityLogService;
import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogCommandRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.vo.EntityReference;
import com.tissue.support.IntegrationTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminActivityLogIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminActivityLogService adminActivityLogService;

    @Autowired
    private ActivityLogCommandRepository activityLogCommandRepository;

    private void saveIssueActivity(String projectKey, String issueKey, Long actorMemberId) {
        activityLogCommandRepository.save(ActivityLog.builder()
                .eventId(UUID.randomUUID())
                .activityType(ActivityType.ISSUE_CREATED)
                .entityReference(EntityReference.forIssue(projectKey, issueKey))
                .data(Map.of())
                .changes(Map.of())
                .actorMemberId(actorMemberId)
                .build());
    }

    @Test
    @DisplayName("lists activity log across all projects regardless of membership, and filters by project")
    void listsAcrossProjects() {
        // given
        saveIssueActivity("PROJ1", "PROJ1-1", 10L);
        saveIssueActivity("PROJ2", "PROJ2-1", 20L);
        em.flush();
        em.clear();

        // when: no filter -> sees both project activities
        Page<ActivityLogResponse> all =
                adminActivityLogService.listActivities(null, null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(all.getContent()).hasSize(2);

        // when: filter by project
        Page<ActivityLogResponse> filterByProj =
                adminActivityLogService.listActivities("PROJ1", null, null, null, PageRequest.of(0, 20));

        // then
        assertThat(filterByProj.getContent()).hasSize(1);
        assertThat(filterByProj.getContent().getFirst().entityReference().getProjectKey())
                .isEqualTo("PROJ1");
    }

    @Test
    @DisplayName("filters by actor")
    void filtersByActor() {
        // given
        saveIssueActivity("PROJ", "PROJ-1", 10L);
        saveIssueActivity("PROJ", "PROJ-2", 99L);
        em.flush();
        em.clear();

        // when
        Page<ActivityLogResponse> filterByActor =
                adminActivityLogService.listActivities(null, null, 99L, null, PageRequest.of(0, 20));

        // then
        assertThat(filterByActor.getContent()).hasSize(1);
        assertThat(filterByActor.getContent().getFirst().actorMemberId()).isEqualTo(99L);
    }
}
