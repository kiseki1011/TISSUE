package com.tissue.notification.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.notification.domain.model.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
}
