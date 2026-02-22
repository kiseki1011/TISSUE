package com.tissue.feature.activitylog.persistence;

import com.tissue.feature.activitylog.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogTestRepository extends JpaRepository<ActivityLog, Long> {
}
