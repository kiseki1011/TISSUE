package com.tissue.feature.activitylog.application.port.repository;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ActivityLogQueryRepository {

    /**
     * Latest issue-scoped activity Instant per project key. Keys with no issue activity are absent
     * from the map.
     */
    Map<String, Instant> findLastActivityAtByProjectKeys(Collection<String> projectKeys);

    /**
     * Latest activity Instant per issue key (comments included). Keys with no activity are absent
     * from the map.
     */
    Map<String, Instant> findLastActivityAtByIssueKeys(Collection<String> issueKeys);

    List<ActivityLog> findAllByIssueKey(String issueKey, @Nullable Long keysetId, int limit);

    List<ActivityLog> findAllBySprintId(Long sprintId, @Nullable Long keysetId, int limit);

    Page<ActivityLog> search(
            @Nullable String projectKey,
            @Nullable String issueKey,
            @Nullable Long actorMemberId,
            @Nullable ActivityType type,
            Pageable pageable);
}
