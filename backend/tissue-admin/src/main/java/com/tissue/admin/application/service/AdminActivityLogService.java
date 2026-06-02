package com.tissue.admin.application.service;

import com.tissue.admin.application.port.usecase.AdminActivityLogUseCase;
import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.repository.ActivityLogQueryRepository;
import com.tissue.feature.activitylog.domain.ActivityType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminActivityLogService implements AdminActivityLogUseCase {

    private final ActivityLogQueryRepository activityLogQueryRepository;

    @Override
    public Page<ActivityLogResponse> listActivities(
            @Nullable String projectKey,
            @Nullable String issueKey,
            @Nullable Long actorMemberId,
            @Nullable ActivityType type,
            Pageable pageable) {
        return activityLogQueryRepository
                .search(projectKey, issueKey, actorMemberId, type, pageable)
                .map(ActivityLogResponse::from);
    }
}
