package com.tissue.admin.application.service;

import com.tissue.admin.adapter.persistence.AdminAuditLogSpecs;
import com.tissue.admin.application.dto.AdminAuditLogResponse;
import com.tissue.admin.application.port.repository.AdminAuditLogRepository;
import com.tissue.admin.application.port.usecase.AdminAuditQueryUseCase;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditLog;
import com.tissue.admin.domain.AdminAuditTargetType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminAuditQueryService implements AdminAuditQueryUseCase {

    private final AdminAuditLogRepository auditLogRepository;

    @Override
    public Page<AdminAuditLogResponse> listAuditLogs(
            @Nullable Long actorMemberId,
            @Nullable AdminAuditAction action,
            @Nullable AdminAuditTargetType targetType,
            Pageable pageable) {
        Specification<AdminAuditLog> spec = AdminAuditLogSpecs.filter(actorMemberId, action, targetType);
        return auditLogRepository.findAll(spec, pageable).map(AdminAuditLogResponse::from);
    }
}
