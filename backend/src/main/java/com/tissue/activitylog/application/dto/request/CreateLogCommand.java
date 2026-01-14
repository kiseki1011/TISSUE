package com.tissue.activitylog.application.dto.request;

import com.tissue.activitylog.domain.enums.ActivityType;
import com.tissue.common.vo.EntityReference;
import java.util.List;
import java.util.UUID;

public record CreateLogCommand(
        UUID eventId, ActivityType activityType, EntityReference reference, Long actorMemberId, List<String> args) {}
