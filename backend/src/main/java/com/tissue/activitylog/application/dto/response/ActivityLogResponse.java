package com.tissue.activitylog.application.dto.response;

import com.tissue.activitylog.domain.ActivityLog;
import com.tissue.activitylog.domain.enums.ActivityType;
import com.tissue.common.dto.FieldChange;
import com.tissue.common.vo.EntityReference;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ActivityLogResponse(
        Long id,
        UUID eventId,
        ActivityType type,
        List<String> args,
        EntityReference entityReference,
        Map<String, FieldChange> changes,
        Long actorMemberId,
        Instant occurredAt) {

    public static ActivityLogResponse from(ActivityLog log) {
        return ActivityLogResponse.builder()
                .id(log.getId())
                .eventId(log.getEventId())
                .type(log.getActivityType())
                .args(log.getArgs())
                .entityReference(log.getEntityReference())
                .changes(log.getChanges())
                .actorMemberId(log.getActorMemberId())
                .occurredAt(log.getCreatedAt())
                .build();
    }
}
