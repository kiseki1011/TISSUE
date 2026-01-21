package com.tissue.notification.domain.service;

import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.NotificationMessage;
import java.util.Map;

public interface NotificationMessageFactory {
    NotificationMessage createMessage(NotificationType type, Map<String, String> data);
}
