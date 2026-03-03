package com.tissue.feature.notification.domain.service;

import com.tissue.feature.notification.domain.Notification;
import com.tissue.feature.notification.domain.enums.NotificationChannel;
import java.util.concurrent.Executor;

public interface NotificationSender {

    NotificationChannel getChannel();

    void send(Notification notification);

    /**
     * Returns the executor to be used for this sender.
     * Different channels may need different concurrency or throttling policies.
     */
    Executor getExecutor();
}
