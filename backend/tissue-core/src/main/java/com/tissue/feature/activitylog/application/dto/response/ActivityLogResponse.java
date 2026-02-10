package com.tissue.feature.activitylog.application.dto.response;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.vo.EntityReference;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record ActivityLogResponse(
        Long id,
        UUID eventId,
        ActivityType type,
        Map<String, String> data,
        EntityReference entityReference,
        Map<String, FieldChange> changes,
        @Nullable Long actorMemberId,
        Instant occurredAt) {

    public static ActivityLogResponse from(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .type(log.getActivityType())
                .data(log.getData())
                .entityReference(log.getEntityReference())
                .changes(log.getChanges())
                .actorMemberId(log.getActorMemberId())
                .occurredAt(log.getCreatedAt())
                .build();
    }
}
