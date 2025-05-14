package com.tissue.api.notification.domain.service.message;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.notification.domain.model.vo.NotificationMessage;

public interface NotificationMessageFactory {
	<T extends DomainEvent> NotificationMessage createMessage(T event);
}
