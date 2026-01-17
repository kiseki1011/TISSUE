package com.tissue.notification.adapter.out.message;

import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.NotificationMessageFactory;
import com.tissue.notification.domain.vo.NotificationMessage;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SimpleNotificationMessageFactory implements NotificationMessageFactory {

    @Override
    public NotificationMessage createMessage(NotificationType type, Map<String, String> data) {
        return new NotificationMessage(data);
    }
}
