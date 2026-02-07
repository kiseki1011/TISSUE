package com.tissue.notification.adapter.web.request;

import com.tissue.notification.application.dto.request.UpdateNotificationPreferenceCommand;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull NotificationType type, @NotNull NotificationChannel channel, boolean enabled) {

    public UpdateNotificationPreferenceCommand toCommand() {
        return new UpdateNotificationPreferenceCommand(type, channel, enabled);
    }
}
