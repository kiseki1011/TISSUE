package com.tissue.notification.domain.exception;

import com.tissue.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("Notification not found"),
    NOT_YOUR_NOTIFICATION("You cannot access this notification");

    private final String defaultMessage;
}
