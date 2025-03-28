package com.tissue.api.notification.domain;

import com.tissue.api.notification.domain.enums.NotificationEntityType;
import com.tissue.api.notification.domain.enums.NotificationType;

public interface NotificationMessageFactory {
	NotificationMessage createMessage(
		NotificationType notificationType,
		NotificationEntityType entityType,
		Long entityId,
		String entityKey,
		Long actorId,
		String workspaceCode
	);
}
