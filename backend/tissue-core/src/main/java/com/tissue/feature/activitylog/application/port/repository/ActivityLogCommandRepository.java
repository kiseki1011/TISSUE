package com.tissue.feature.activitylog.application.port.repository;

import com.tissue.feature.activitylog.domain.ActivityLog;
import java.util.List;
import org.springframework.data.repository.Repository;

public interface ActivityLogCommandRepository extends Repository<ActivityLog, Long> {

    ActivityLog save(ActivityLog activityLog);

    // TODO: ActivityLogQueryRepository로 이동
    List<ActivityLog> findAll();
}
