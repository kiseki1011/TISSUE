package com.tissue.feature.notification.sender;

import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import com.tissue.feature.notification.domain.service.NotificationSender;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {}
}
