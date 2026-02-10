package com.tissue.feature.notification.application.dto.response;

import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import lombok.Builder;

@Builder
public record NotificationPreferenceResponse(NotificationType type, NotificationChannel channel, boolean enabled) {}
