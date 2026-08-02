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
}
