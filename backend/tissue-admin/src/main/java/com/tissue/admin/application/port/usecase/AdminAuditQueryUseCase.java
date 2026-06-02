package com.tissue.admin.application.port.usecase;

import com.tissue.admin.application.dto.AdminAuditLogResponse;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditTargetType;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminAuditQueryUseCase {

    Page<AdminAuditLogResponse> listAuditLogs(
            @Nullable Long actorMemberId,
            @Nullable AdminAuditAction action,
            @Nullable AdminAuditTargetType targetType,
            Pageable pageable);
}
