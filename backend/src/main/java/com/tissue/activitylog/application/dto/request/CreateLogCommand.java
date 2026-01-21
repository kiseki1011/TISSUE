package com.tissue.activitylog.application.dto.request;

import com.tissue.activitylog.domain.ActivityType;
import com.tissue.common.vo.EntityReference;
import java.util.Map;
import java.util.UUID;

public record CreateLogCommand(
        UUID eventId,
        ActivityType activityType,
        EntityReference reference,
        Long actorMemberId,
        Map<String, String> data) {}
