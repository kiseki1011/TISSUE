package com.tissue.notification.domain.service.sender;

import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class InAppSender implements NotificationSender {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(Notification notification) {}
}
