package com.tissue.notification.web.request;

import com.tissue.feature.notification.application.dto.request.UpdateNotificationPreferenceCommand;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull NotificationType type, @NotNull NotificationChannel channel, boolean enabled) {

    public UpdateNotificationPreferenceCommand toCommand() {
        return new UpdateNotificationPreferenceCommand(type, channel, enabled);
    }
}
