package com.tissue.feature.activitylog.application.port.out;

import com.tissue.feature.activitylog.domain.ActivityLog;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);

    List<ActivityLog> findAll();
}
