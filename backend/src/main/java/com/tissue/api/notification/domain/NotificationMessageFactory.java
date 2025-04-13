package com.tissue.api.notification.domain;

import com.tissue.api.common.event.DomainEvent;

public interface NotificationMessageFactory {
	<T extends DomainEvent> NotificationMessage createMessage(T event);
}
