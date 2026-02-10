package com.tissue.feature.activitylog.application.port.out;

import com.tissue.feature.activitylog.domain.ActivityLog;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface ActivityLogQueryRepository {

    List<ActivityLog> findByIssue(String workspaceKey, String issueKey, @Nullable Long cursorId, int limit);

    List<ActivityLog> findBySprint(String workspaceKey, Long sprintId, @Nullable Long cursorId, int limit);
}
