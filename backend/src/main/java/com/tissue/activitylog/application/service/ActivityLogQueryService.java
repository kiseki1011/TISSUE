package com.tissue.activitylog.application.service;

import com.tissue.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.activitylog.application.port.out.ActivityLogQueryRepository;
import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.common.dto.CursorPageResponse;
import com.tissue.project.application.dto.ProjectMemberContext;
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

    public CursorPageResponse<ActivityLogResponse> getIssueActivities(
            ProjectMemberContext actor, String issueKey, @Nullable Long cursorId, int limit) {

        List<ActivityLog> logs = queryRepository.findByIssue(actor.workspaceKey(), issueKey, cursorId, limit);
        return createResponse(logs);
    }

    public CursorPageResponse<ActivityLogResponse> getSprintActivities(
            ProjectMemberContext actor, Long sprintId, @Nullable Long cursorId, int limit) {

        List<ActivityLog> logs = queryRepository.findBySprint(actor.workspaceKey(), sprintId, cursorId, limit);
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
