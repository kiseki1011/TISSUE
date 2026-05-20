package com.tissue.feature.notification.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Notification not found"),
    NOT_YOUR_NOTIFICATION(HttpStatus.FORBIDDEN, "You cannot access this notification");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
