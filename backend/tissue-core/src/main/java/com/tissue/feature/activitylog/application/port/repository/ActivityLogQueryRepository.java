package com.tissue.feature.activitylog.application.port.repository;

import com.tissue.feature.activitylog.domain.ActivityLog;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface ActivityLogQueryRepository {

    List<ActivityLog> findAllByIssueKey(String issueKey, @Nullable Long keysetId, int limit);

    List<ActivityLog> findAllBySprintId(Long sprintId, @Nullable Long keysetId, int limit);
}
