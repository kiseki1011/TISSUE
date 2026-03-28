package com.tissue.feature.activitylog.domain;

import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.entity.BaseDateEntity;
import com.tissue.shared.vo.EntityReference;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class ActivityLog extends BaseDateEntity {

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Embedded
    private EntityReference entityReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "activity_data", columnDefinition = "jsonb")
    private Map<String, String> data = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "changes", columnDefinition = "jsonb")
    private Map<String, FieldChange> changes = new HashMap<>();

    @Column(name = "actor_member_id")
    @Nullable
    private Long actorMemberId;

    @SuppressWarnings("NullAway.Init")
    protected ActivityLog() {}

    @Builder
    public ActivityLog(
            UUID eventId,
            ActivityType activityType,
            EntityReference entityReference,
            Map<String, String> data,
            Map<String, FieldChange> changes,
            @Nullable Long actorMemberId) {

        this.eventId = eventId;
        this.activityType = activityType;
        this.entityReference = entityReference;
        this.data = data;
        this.changes = changes;
        this.actorMemberId = actorMemberId;
    }
}
