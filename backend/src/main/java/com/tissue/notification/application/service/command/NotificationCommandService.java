package com.tissue.notification.application.service.command;

import com.tissue.notification.infrastructure.repository.NotificationRepository;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
}
