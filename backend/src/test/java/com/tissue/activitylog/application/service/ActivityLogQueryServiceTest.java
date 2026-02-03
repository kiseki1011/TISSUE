package com.tissue.activitylog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tissue.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.activitylog.application.port.out.ActivityLogQueryRepository;
import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.activitylog.domain.ActivityType;
import com.tissue.common.dto.CursorPageResponse;
import com.tissue.global.vo.EntityReference;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ActivityLogQueryServiceTest {

    @Mock
    ActivityLogQueryRepository queryRepository;

    @Mock
    ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    ActivityLogQueryService sut;

    @Nested
    @DisplayName("get issue activities")
    class GetIssueActivities {

        @Test
        @DisplayName("success: checks auth and returns logs")
        void success_GetIssueActivities() {
            String workspaceKey = "TESTWS";
            String projectKey = "TESTPROJ";
            String issueKey = "TESTPROJ-1";
            Long memberId = 1L;
            Long cursorId = null;
            int limit = 20;
            ProjectMemberContext actor = new ProjectMemberContext(
                    1L, memberId, 1L, workspaceKey, 1L, projectKey, "name", WorkspaceRole.MEMBER);

            ActivityLog log1 = ActivityLog.builder()
                    .eventId(UUID.randomUUID())
                    .activityType(ActivityType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue(workspaceKey, projectKey, issueKey))
                    .actorMemberId(memberId)
                    .data(Map.of())
                    .build();
            ReflectionTestUtils.setField(log1, "id", 10L);

            given(queryRepository.findByIssue(actor.workspaceKey(), issueKey, cursorId, limit))
                    .willReturn(List.of(log1));

            CursorPageResponse<ActivityLogResponse> response = sut.getIssueActivities(actor, issueKey, cursorId, limit);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().get(0).id()).isEqualTo(10L);
            assertThat(response.nextCursorId()).isEqualTo(10L);
        }
    }

    @Nested
    @DisplayName("get sprint activities")
    class GetSprintActivities {

        @Test
        @DisplayName("success: checks auth and returns logs")
        void success_GetSprintActivities() {
            String workspaceKey = "TESTWS";
            String projectKey = "TESTPROJ";
            Long sprintId = 200L;
            Long memberId = 1L;
            Long cursorId = null;
            int limit = 20;
            ProjectMemberContext actor = new ProjectMemberContext(
                    1L, memberId, 1L, workspaceKey, 1L, projectKey, "name", WorkspaceRole.MEMBER);

            given(queryRepository.findBySprint(actor.workspaceKey(), sprintId, cursorId, limit))
                    .willReturn(Collections.emptyList());

            CursorPageResponse<ActivityLogResponse> response =
                    sut.getSprintActivities(actor, sprintId, cursorId, limit);

            assertThat(response.content()).isEmpty();
            assertThat(response.nextCursorId()).isNull();
        }
    }
}
