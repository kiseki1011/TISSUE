package com.tissue.feature.activitylog.application.port.repository;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityLogQueryRepository {

    List<ActivityLog> findAllByIssueKey(String issueKey, @Nullable Long keysetId, int limit);

    List<ActivityLog> findAllBySprintId(Long sprintId, @Nullable Long keysetId, int limit);

    Page<ActivityLog> search(
            @Nullable String projectKey,
            @Nullable String issueKey,
            @Nullable Long actorMemberId,
            @Nullable ActivityType type,
            Pageable pageable);
}
