package com.tissue.activitylog.application.dto.request;

import com.tissue.activitylog.domain.ActivityType;
import com.tissue.common.dto.FieldChange;
import com.tissue.global.vo.EntityReference;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CreateLogWithDiffCommand(
    UUID eventId,
    ActivityType activityType,
    EntityReference reference,
    @Nullable Long actorMemberId,
    Map<String, String> data,
    Map<String, FieldChange> changes) {

}
