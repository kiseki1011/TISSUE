package com.tissue.sprint.application.service.event;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.event.SprintCompletedEvent;
import com.tissue.sprint.domain.event.SprintStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishSprintStarted(Sprint sprint, ProjectMemberContext actor) {
        eventPublisher.publishEvent(SprintStartedEvent.create(
                actor.workspaceKey(),
                actor.projectKey(),
                sprint.getId(),
                sprint.getTitle(),
                actor.memberId(),
                actor.displayName()));
    }

    public void publishSprintCompleted(Sprint sprint, ProjectMemberContext actor) {
        eventPublisher.publishEvent(SprintCompletedEvent.create(
                actor.workspaceKey(),
                actor.projectKey(),
                sprint.getId(),
                sprint.getTitle(),
                sprint.getStartedAt(),
                sprint.getCompletedAt(),
                actor.memberId(),
                actor.displayName()));
    }
}
