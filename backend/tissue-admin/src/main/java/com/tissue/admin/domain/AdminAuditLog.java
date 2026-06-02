package com.tissue.admin.domain;

import com.tissue.shared.entity.BaseDateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(name = "admin_audit_log")
public class AdminAuditLog extends BaseDateEntity {

    @Column(name = "actor_member_id", nullable = false)
    private Long actorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private AdminAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private AdminAuditTargetType targetType;

    @Nullable
    @Column(name = "target_ref")
    private String targetRef;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, String> data = new HashMap<>();

    @SuppressWarnings("NullAway.Init")
    protected AdminAuditLog() {}

    private AdminAuditLog(
            Long actorMemberId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            @Nullable String targetRef,
            Map<String, String> data) {
        this.actorMemberId = actorMemberId;
        this.action = action;
        this.targetType = targetType;
        this.targetRef = targetRef;
        this.data = data;
    }

    public static AdminAuditLog create(
            Long actorMemberId,
            AdminAuditAction action,
            AdminAuditTargetType targetType,
            @Nullable String targetRef,
            Map<String, String> data) {
        return new AdminAuditLog(actorMemberId, action, targetType, targetRef, data);
    }
}
