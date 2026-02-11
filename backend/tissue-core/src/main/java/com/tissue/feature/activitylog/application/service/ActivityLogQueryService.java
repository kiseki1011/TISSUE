package com.tissue.feature.activitylog.application.service;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.shared.dto.CursorPageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityLogQueryService {

    private final ActivityLogQueryRepository activityLogQueryRepository;

    public CursorPageResponse<ActivityLogResponse> getIssueActivities(
            ProjectMemberContext actor, String issueKey, @Nullable Long cursorId, int limit) {
        List<ActivityLog> logs =
                activityLogQueryRepository.findByIssue(actor.workspaceKey(), issueKey, cursorId, limit);
        return createResponse(logs);
    }

    public CursorPageResponse<ActivityLogResponse> getSprintActivities(
            ProjectMemberContext actor, Long sprintId, @Nullable Long cursorId, int limit) {
        List<ActivityLog> logs =
                activityLogQueryRepository.findBySprint(actor.workspaceKey(), sprintId, cursorId, limit);
        return createResponse(logs);
    }

    private CursorPageResponse<ActivityLogResponse> createResponse(List<ActivityLog> logs) {
        List<ActivityLogResponse> content =
                logs.stream().map(ActivityLogResponse::from).toList();

        Long nextCursorId = null;
        if (!content.isEmpty()) {
            nextCursorId = content.getLast().id();
        }

        return CursorPageResponse.of(content, nextCursorId);
    }
}
