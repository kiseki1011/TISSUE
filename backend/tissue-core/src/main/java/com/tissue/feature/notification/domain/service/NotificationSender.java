package com.tissue.feature.notification.domain.service;

import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationChannel;

public interface NotificationSender {
    NotificationChannel getChannel();

    void send(Notification notification);
}
