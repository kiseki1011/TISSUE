package com.tissue.activitylog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.application.service.ActivityLogQueryService;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.shared.dto.CursorPageResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.vo.EntityReference;
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
    WorkspaceMemberFinder workspaceMemberFinder;

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

            ActivityLog log1 = ActivityLog.builder()
                    .eventId(UUID.randomUUID())
                    .activityType(ActivityType.ISSUE_CREATED)
                    .entityReference(EntityReference.forIssue(workspaceKey, projectKey, issueKey))
                    .actorMemberId(memberId)
                    .data(Map.of())
                    .build();
            ReflectionTestUtils.setField(log1, "id", 10L);

            given(queryRepository.findAllByWorkspaceKeyAndIssueKey(workspaceKey, issueKey, cursorId, limit))
                    .willReturn(List.of(log1));

            CursorPageResponse<ActivityLogResponse> response = sut.getIssueActivities(
                    IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberId, cursorId, limit);

            assertThat(response.content()).hasSize(1);
            assertThat(response.content().getFirst().id()).isEqualTo(10L);
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
            Long sprintId = 200L;
            Long memberId = 1L;
            Long cursorId = null;
            int limit = 20;

            given(queryRepository.findAllByWorkspaceKeyAndSprintId(workspaceKey, sprintId, cursorId, limit))
                    .willReturn(Collections.emptyList());

            CursorPageResponse<ActivityLogResponse> response =
                    sut.getSprintActivities(workspaceKey, sprintId, memberId, cursorId, limit);

            assertThat(response.content()).isEmpty();
            assertThat(response.nextCursorId()).isNull();
        }
    }
}
