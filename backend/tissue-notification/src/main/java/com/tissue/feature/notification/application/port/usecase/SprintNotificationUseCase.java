package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;

public interface SprintNotificationUseCase {

    void handleSprintStarted(SprintStartedEvent event);

    void handleSprintCompleted(SprintCompletedEvent event);
}
