package com.tissue.api.notification.domain.service.sender;

import com.tissue.api.notification.domain.enums.NotificationChannel;
import com.tissue.api.notification.domain.model.Notification;

public interface NotificationSender {
	NotificationChannel getChannel();

	void send(Notification notification);
}
