package com.tissue.notification.exception;

import com.tissue.common.exception.base.ResourceNotFoundException;

public class NotificationNotFoundException extends ResourceNotFoundException {

	public NotificationNotFoundException(Long notificationId) {
		super("Notification not found for notification id '%d'".formatted(notificationId));
		addContext("notificationId", notificationId);
	}
}
