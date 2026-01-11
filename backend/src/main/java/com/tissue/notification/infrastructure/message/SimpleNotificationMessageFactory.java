package com.tissue.notification.infrastructure.message;

import com.tissue.notification.domain.enums.NotificationType;
import com.tissue.notification.domain.service.message.NotificationMessageFactory;
import com.tissue.notification.domain.vo.NotificationMessage;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SimpleNotificationMessageFactory implements NotificationMessageFactory {

    @Override
    public NotificationMessage createMessage(NotificationType type, Object... args) {
        List<String> stringArgs = Arrays.stream(args).map(String::valueOf).toList();

        return new NotificationMessage(stringArgs);
    }
}
