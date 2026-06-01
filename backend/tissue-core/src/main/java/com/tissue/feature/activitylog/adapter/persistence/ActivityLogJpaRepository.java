package com.tissue.feature.activitylog.adapter.persistence;

import com.tissue.feature.activitylog.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityLogJpaRepository
        extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {}
