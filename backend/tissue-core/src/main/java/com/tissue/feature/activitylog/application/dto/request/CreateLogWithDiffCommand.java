package com.tissue.feature.activitylog.application.dto.request;

import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.vo.EntityReference;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CreateLogWithDiffCommand(
        UUID eventId,
        ActivityType activityType,
        EntityReference reference,
        @Nullable Long actorMemberId,
        Map<String, String> data,
        Map<String, FieldChange> changes) {}
