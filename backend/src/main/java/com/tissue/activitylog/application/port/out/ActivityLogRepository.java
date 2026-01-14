package com.tissue.activitylog.application.port.out;

import com.tissue.activitylog.domain.ActivityLog;
import org.springframework.data.repository.Repository;

public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);
}
