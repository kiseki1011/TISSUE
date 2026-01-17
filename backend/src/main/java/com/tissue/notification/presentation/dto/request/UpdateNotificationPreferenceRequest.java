package com.tissue.notification.presentation.dto.request;

import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull NotificationType type, @NotNull NotificationChannel channel, boolean enabled) {}
