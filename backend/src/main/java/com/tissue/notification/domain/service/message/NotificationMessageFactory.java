package com.tissue.notification.domain.service.message;

import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.vo.NotificationMessage;

public interface NotificationMessageFactory {
    NotificationMessage createMessage(NotificationType type, Object... args);
}
