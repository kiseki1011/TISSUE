package com.tissue.activitylog.application.dto.request;

import com.tissue.activitylog.domain.enums.ActivityType;
import com.tissue.common.dto.FieldChange;
import com.tissue.common.vo.EntityReference;
import java.util.Map;
import java.util.UUID;

public record CreateLogWithDiffCommand(
        UUID eventId,
        ActivityType activityType,
        EntityReference reference,
        Long actorMemberId,
        Map<String, String> data,
        Map<String, FieldChange> changes) {}
