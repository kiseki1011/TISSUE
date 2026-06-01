package com.tissue.feature.notification.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.NOTIFICATION_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class NotificationNotFoundException extends ResourceNotFoundException {

    public NotificationNotFoundException(Long notificationId) {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        addContext(NOTIFICATION_ID, notificationId);
    }
}
