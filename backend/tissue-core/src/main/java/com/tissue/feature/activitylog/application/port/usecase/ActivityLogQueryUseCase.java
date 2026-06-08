package com.tissue.feature.activitylog.application.port.usecase;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.IssueIdentifier;
import org.jspecify.annotations.Nullable;

public interface ActivityLogQueryUseCase {

    CursorPage<ActivityLogResponse> getIssueActivities(
            IssueIdentifier iid, Long actorMemberId, @Nullable String cursor, int limit);

    CursorPage<ActivityLogResponse> getSprintActivities(
            Long sprintId, Long actorMemberId, @Nullable String cursor, int limit);
}
