package com.tissue.feature.notification.adapter.event;

import com.tissue.feature.notification.application.port.usecase.ProjectNotificationUseCase;
import com.tissue.feature.project.domain.event.ProjectRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProjectNotificationEventAdapter {

    private final ProjectNotificationUseCase useCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProjectRoleChanged(ProjectRoleChangedEvent event) {
        useCase.handleProjectRoleChanged(event);
    }
}
