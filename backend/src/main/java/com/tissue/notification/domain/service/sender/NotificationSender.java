package com.tissue.notification.domain.service.sender;

import com.tissue.notification.domain.enums.NotificationChannel;
import com.tissue.notification.domain.model.Notification;

public interface NotificationSender {
	NotificationChannel getChannel();

	void send(Notification notification);
}
