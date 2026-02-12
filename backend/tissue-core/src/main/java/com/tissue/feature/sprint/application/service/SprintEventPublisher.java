package com.tissue.feature.sprint.application.service;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishSprintStarted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintStartedEvent.create(
                actor.getWorkspaceKey(),
                actor.getProjectKey(),
                sprint.getId(),
                sprint.getTitle(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }

    public void publishSprintCompleted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintCompletedEvent.create(
                actor.getWorkspaceKey(),
                actor.getProjectKey(),
                sprint.getId(),
                sprint.getTitle(),
                sprint.getStartedAt(),
                sprint.getCompletedAt(),
                actor.getMemberId(),
                actor.getWorkspaceMember().getDisplayName()));
    }
}
