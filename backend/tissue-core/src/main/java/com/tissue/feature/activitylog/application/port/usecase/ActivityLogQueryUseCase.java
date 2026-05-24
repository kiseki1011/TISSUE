package com.tissue.feature.activitylog.application.port.usecase;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.KeysetPageResponse;
import org.jspecify.annotations.Nullable;

public interface ActivityLogQueryUseCase {

    KeysetPageResponse<ActivityLogResponse> getIssueActivities(
            IssueIdentifier iid, Long actorMemberId, @Nullable Long keysetId, int limit);

    KeysetPageResponse<ActivityLogResponse> getSprintActivities(
            String workspaceKey, Long sprintId, Long actorMemberId, @Nullable Long keysetId, int limit);
}
