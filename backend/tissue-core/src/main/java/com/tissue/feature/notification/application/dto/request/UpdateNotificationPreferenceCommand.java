package com.tissue.feature.notification.application.dto.request;

import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;

public record UpdateNotificationPreferenceCommand(
        NotificationType type, NotificationChannel channel, boolean enabled) {}
