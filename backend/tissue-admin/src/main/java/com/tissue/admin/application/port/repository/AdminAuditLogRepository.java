package com.tissue.admin.application.port.repository;

import com.tissue.admin.domain.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

public interface AdminAuditLogRepository
        extends Repository<AdminAuditLog, Long>, JpaSpecificationExecutor<AdminAuditLog> {

    AdminAuditLog save(AdminAuditLog auditLog);
}
