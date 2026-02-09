package com.tissue.activitylog.application.port.out;

import com.tissue.activitylog.domain.ActivityLog;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface ActivityLogQueryRepository {

    List<ActivityLog> findByIssue(String workspaceKey, String issueKey, @Nullable Long cursorId, int limit);

    List<ActivityLog> findBySprint(String workspaceKey, Long sprintId, @Nullable Long cursorId, int limit);
}
