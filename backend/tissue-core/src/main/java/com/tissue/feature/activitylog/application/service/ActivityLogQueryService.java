package com.tissue.feature.activitylog.application.service;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.KeysetPageResponse;
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

    public KeysetPageResponse<ActivityLogResponse> getIssueActivities(
            IssueIdentifier iid, Long memberId, @Nullable Long keysetId, int limit) {
        workspaceMemberFinder.getWithWorkspace(iid.workspaceKey(), memberId);

        List<ActivityLog> logs = activityLogQueryRepository.findAllByWorkspaceKeyAndIssueKey(
                iid.workspaceKey(), iid.issueKey(), keysetId, limit);
        return createResponse(logs);
    }

    public KeysetPageResponse<ActivityLogResponse> getSprintActivities(
            String workspaceKey, Long sprintId, Long memberId, @Nullable Long keysetId, int limit) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, memberId);

        List<ActivityLog> logs =
                activityLogQueryRepository.findAllByWorkspaceKeyAndSprintId(workspaceKey, sprintId, keysetId, limit);
        return createResponse(logs);
    }

    private KeysetPageResponse<ActivityLogResponse> createResponse(List<ActivityLog> logs) {
        List<ActivityLogResponse> content =
                logs.stream().map(ActivityLogResponse::from).toList();

        Long nextKeysetId = null;
        if (!content.isEmpty()) {
            nextKeysetId = content.getLast().id();
        }

        return KeysetPageResponse.of(content, nextKeysetId);
    }
}
