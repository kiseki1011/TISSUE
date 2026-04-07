package com.tissue.feature.activitylog.application.dto.response;

import com.tissue.feature.activitylog.domain.ActivityLog;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.vo.EntityReference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "A single activity log entry representing an event that occurred on an issue or sprint.")
@Builder
public record ActivityLogResponse(
        @Schema(example = "1") Long id,

        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID eventId,

        @Schema(example = "ISSUE_UPDATED") ActivityType type,
        Map<String, String> data,
        EntityReference entityReference,

        @Schema(description = "Field name to before/after value", example = """
                {"priority": {"from": "NORMAL", "to": "MAJOR"}}""")
        Map<String, FieldChange> changes,

        @Schema(example = "123") @Nullable Long actorMemberId,
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
