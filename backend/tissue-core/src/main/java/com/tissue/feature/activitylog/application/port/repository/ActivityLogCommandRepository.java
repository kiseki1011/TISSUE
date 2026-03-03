package com.tissue.feature.activitylog.application.port.repository;

import com.tissue.feature.activitylog.domain.ActivityLog;
import org.springframework.data.repository.Repository;

public interface ActivityLogCommandRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);
}
