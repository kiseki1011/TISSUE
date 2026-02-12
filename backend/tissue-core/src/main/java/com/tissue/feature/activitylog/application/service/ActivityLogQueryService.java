package com.tissue.feature.activitylog.application.service;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.shared.dto.CursorPageResponse;
import com.tissue.shared.dto.IssueIdentifier;
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
    private final WorkspaceMemberFinder workspaceMemberFinder;

    public CursorPageResponse<ActivityLogResponse> getIssueActivities(
            IssueIdentifier issueIdentifier, Long memberId, @Nullable Long cursorId, int limit) {
        workspaceMemberFinder.getBy(issueIdentifier.workspaceKey(), memberId);

        List<ActivityLog> logs = activityLogQueryRepository.findAllByWorkspaceKeyAndIssueKey(
                issueIdentifier.workspaceKey(), issueIdentifier.issueKey(), cursorId, limit);
        return createResponse(logs);
    }

    public CursorPageResponse<ActivityLogResponse> getSprintActivities(
            String workspaceKey, Long sprintId, Long memberId, @Nullable Long cursorId, int limit) {
        workspaceMemberFinder.getBy(workspaceKey, memberId);

        List<ActivityLog> logs =
                activityLogQueryRepository.findAllByWorkspaceKeyAndSprintId(workspaceKey, sprintId, cursorId, limit);
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
