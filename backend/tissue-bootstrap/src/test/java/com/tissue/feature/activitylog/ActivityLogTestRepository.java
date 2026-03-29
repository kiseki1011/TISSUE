package com.tissue.feature.activitylog;

import com.tissue.feature.activitylog.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogTestRepository extends JpaRepository<ActivityLog, Long> {}
