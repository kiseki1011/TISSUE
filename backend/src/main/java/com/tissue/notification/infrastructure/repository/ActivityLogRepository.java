package com.tissue.notification.infrastructure.repository;

import com.tissue.notification.domain.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {}
