package com.tissue.notification.domain.service.sender;

import com.tissue.notification.domain.Notification;
import com.tissue.notification.domain.enums.NotificationChannel;

public interface NotificationSender {
    NotificationChannel getChannel();

    void send(Notification notification);
}
