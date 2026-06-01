package com.tissue.feature.notification.adapter.event;

import com.tissue.feature.notification.application.port.usecase.SprintNotificationUseCase;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SprintNotificationEventAdapter {

    private final SprintNotificationUseCase useCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintStarted(SprintStartedEvent event) {
        useCase.handleSprintStarted(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSprintCompleted(SprintCompletedEvent event) {
        useCase.handleSprintCompleted(event);
    }
}
