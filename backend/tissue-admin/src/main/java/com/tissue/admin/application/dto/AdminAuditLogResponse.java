package com.tissue.admin.application.dto;

import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditLog;
import com.tissue.admin.domain.AdminAuditTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "A privileged admin action audit entry")
@Builder
public record AdminAuditLogResponse(
        Long id,

        @Schema(description = "Member id of the SUPER_ADMIN who performed the action")
        Long actorMemberId,

        AdminAuditAction action,
        AdminAuditTargetType targetType,

        @Schema(description = "Identifier of the target (e.g. member id or project key)") @Nullable
        String targetRef,

        Map<String, String> data,
        Instant occurredAt) {

    public static AdminAuditLogResponse from(AdminAuditLog log) {
        return AdminAuditLogResponse.builder()
                .id(log.getId())
                .actorMemberId(log.getActorMemberId())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetRef(log.getTargetRef())
                .data(log.getData())
                .occurredAt(log.getCreatedAt())
                .build();
    }
}
