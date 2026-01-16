package com.tissue.notification.presentation.dto.response;

import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.enums.NotificationType;
import lombok.Builder;

@Builder
public record NotificationPreferenceResponse(NotificationType type, NotificationChannel channel, boolean enabled) {}
