package com.tissue.notification.adapter.sender;

import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.service.NotificationSender;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {
    }
}
