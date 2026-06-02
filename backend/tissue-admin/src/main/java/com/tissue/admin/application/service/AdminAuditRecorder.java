package com.tissue.admin.application.service;

import com.tissue.admin.application.port.repository.AdminAuditLogRepository;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditLog;
import com.tissue.admin.domain.AdminAuditTargetType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuditRecorder {

    private final AdminAuditLogRepository auditLogRepository;

    public void recordMemberAction(
            Long actorMemberId, AdminAuditAction action, Long targetMemberId, Map<String, String> data) {
        auditLogRepository.save(AdminAuditLog.create(
                actorMemberId, action, AdminAuditTargetType.MEMBER, String.valueOf(targetMemberId), data));
    }
}
