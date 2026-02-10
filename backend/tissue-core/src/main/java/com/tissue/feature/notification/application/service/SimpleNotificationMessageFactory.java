package com.tissue.feature.notification.application.service;

import com.tissue.feature.notification.domain.enums.NotificationType;
import com.tissue.feature.notification.domain.service.NotificationMessageFactory;
import com.tissue.feature.notification.domain.vo.NotificationMessage;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SimpleNotificationMessageFactory implements NotificationMessageFactory {

    @Override
    public NotificationMessage createMessage(NotificationType type, Map<String, String> data) {
        return new NotificationMessage(data);
    }
}
