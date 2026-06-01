package com.tissue.feature.notification.domain.service;

import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import java.util.Map;

public interface NotificationMessageFactory {
    NotificationMessage createMessage(NotificationType type, Map<String, String> data);
}
