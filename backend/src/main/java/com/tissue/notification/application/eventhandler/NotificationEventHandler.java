package com.tissue.notification.application.eventhandler;

import com.tissue.notification.application.service.command.NotificationCommandService;
import com.tissue.notification.application.service.command.NotificationProcessor;
import com.tissue.notification.application.service.command.NotificationTargetService;
import com.tissue.notification.infrastructure.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationCommandService commandService;
    private final NotificationProcessor notificationProcessor;
    private final NotificationTargetService targetResolver;
    private final ActivityLogRepository activityLogRepository;
}
