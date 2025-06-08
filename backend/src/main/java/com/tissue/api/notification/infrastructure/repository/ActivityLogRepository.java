package com.tissue.api.notification.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.notification.domain.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
