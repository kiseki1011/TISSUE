package com.tissue.admin.application.port.usecase;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.domain.ActivityType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminActivityLogUseCase {

    Page<ActivityLogResponse> listActivities(
            @Nullable String projectKey,
            @Nullable String issueKey,
            @Nullable Long actorMemberId,
            @Nullable ActivityType type,
            Pageable pageable);
}
