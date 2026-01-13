package com.tissue.sprint.application.service.event;

import com.tissue.project.domain.ProjectMember;
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

    public void publishSprintStarted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintStartedEvent.create(
                sprint.getProject().getWorkspaceKey(),
                sprint.getProject().getKey(),
                sprint.getId(),
                sprint.getTitle(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishSprintCompleted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintCompletedEvent.create(
                sprint.getProject().getWorkspaceKey(),
                sprint.getProject().getKey(),
                sprint.getId(),
                sprint.getTitle(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }
}
