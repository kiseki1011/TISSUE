package com.tissue.notification.application.dto.request;

import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;

public record UpdateNotificationPreferenceCommand(
        NotificationType type, NotificationChannel channel, boolean enabled) {}
