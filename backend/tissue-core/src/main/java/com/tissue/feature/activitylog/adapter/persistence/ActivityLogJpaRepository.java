package com.tissue.feature.activitylog.adapter.persistence;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityLogJpaRepository
        extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

    @Query("""
            SELECT al.entityReference.projectKey AS projectKey, MAX(al.createdAt) AS lastActivityAt
            FROM ActivityLog al
            WHERE al.entityReference.projectKey IN :projectKeys
              AND al.activityType IN :types
            GROUP BY al.entityReference.projectKey
            """)
    List<ProjectLastActivityRow> findLastActivityByProjectKeys(
            @Param("projectKeys") Collection<String> projectKeys, @Param("types") Collection<ActivityType> types);

    // No activity_type filter: issue_key is only populated on issue-scoped rows (comments included), so
    // scoping by issue_key already excludes sprint/project events. This also keeps the aggregate
    // index-only against idx_activity_log_issue_key_created_at (issue_key, created_at DESC).
    @Query("""
            SELECT al.entityReference.issueKey AS issueKey, MAX(al.createdAt) AS lastActivityAt
            FROM ActivityLog al
            WHERE al.entityReference.issueKey IN :issueKeys
            GROUP BY al.entityReference.issueKey
            """)
    List<IssueLastActivityRow> findLastActivityByIssueKeys(@Param("issueKeys") Collection<String> issueKeys);
}
