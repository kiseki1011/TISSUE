package com.tissue.activitylog.application.service;

import com.tissue.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.activitylog.application.port.out.ActivityLogQueryRepository;
import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.common.dto.CursorPageResponse;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityLogQueryService {

    private final ActivityLogQueryRepository queryRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final CurrentMemberProvider currentMemberProvider;

    public CursorPageResponse<ActivityLogResponse> getIssueActivities(
            String workspaceKey, String projectKey, String issueKey, @Nullable Long cursorId, int limit) {

        projectAuthorizationService.requireProjectViewer(
                workspaceKey, projectKey, currentMemberProvider.getCurrentMemberId());

        List<ActivityLog> logs = queryRepository.findByIssue(workspaceKey, issueKey, cursorId, limit);
        return createResponse(logs);
    }

    public CursorPageResponse<ActivityLogResponse> getSprintActivities(
            String workspaceKey, String projectKey, Long sprintId, @Nullable Long cursorId, int limit) {

        projectAuthorizationService.requireProjectViewer(
                workspaceKey, projectKey, currentMemberProvider.getCurrentMemberId());

        List<ActivityLog> logs = queryRepository.findBySprint(workspaceKey, sprintId, cursorId, limit);
        return createResponse(logs);
    }

    private CursorPageResponse<ActivityLogResponse> createResponse(List<ActivityLog> logs) {
        List<ActivityLogResponse> content =
                logs.stream().map(ActivityLogResponse::from).toList();

        Long nextCursorId = null;
        if (!content.isEmpty()) {
            nextCursorId = content.get(content.size() - 1).id();
        }

        return CursorPageResponse.of(content, nextCursorId);
    }
}
