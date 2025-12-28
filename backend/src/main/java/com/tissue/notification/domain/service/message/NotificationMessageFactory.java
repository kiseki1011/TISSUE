package com.tissue.notification.domain.service.message;

import com.tissue.common.event.DomainEvent;
import com.tissue.notification.domain.model.vo.NotificationMessage;

public interface NotificationMessageFactory {
    <T extends DomainEvent> NotificationMessage createMessage(T event);
}
