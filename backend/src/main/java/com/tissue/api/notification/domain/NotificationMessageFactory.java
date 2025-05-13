package com.tissue.api.notification.domain;

import com.tissue.api.common.event.DomainEvent;
import com.tissue.api.notification.domain.vo.NotificationMessage;

public interface NotificationMessageFactory {
	<T extends DomainEvent> NotificationMessage createMessage(T event);
}
