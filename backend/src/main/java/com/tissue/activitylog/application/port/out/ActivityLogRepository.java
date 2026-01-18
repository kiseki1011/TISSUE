package com.tissue.activitylog.application.port.out;

import com.tissue.activitylog.domain.ActivityLog;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface ActivityLogRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);

    List<ActivityLog> findAll();
}
