package com.tissue.feature.notification.application.port.usecase;

public interface NotificationCommandUseCase {

    void readNotification(Long notificationId, Long actorMemberId);

    void readAllNotifications(String workspaceKey, Long actorMemberId);
}
