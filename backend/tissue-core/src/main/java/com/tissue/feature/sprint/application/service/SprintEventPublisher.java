package com.tissue.feature.sprint.application.service;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.event.SprintCancelledEvent;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintCreatedEvent;
import com.tissue.feature.sprint.domain.event.SprintDeletedEvent;
import com.tissue.feature.sprint.domain.event.SprintIssuesAddedEvent;
import com.tissue.feature.sprint.domain.event.SprintIssuesRemovedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import com.tissue.feature.sprint.domain.event.SprintUpdatedEvent;
import com.tissue.shared.dto.FieldChange;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishSprintCreated(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintCreatedEvent.create(
                actor.getProjectKey(), sprint.getId(), sprint.getTitle(), actor.getMemberId(), actor.getDisplayName()));
    }

    public void publishSprintUpdated(Sprint sprint, Map<String, FieldChange> changes, ProjectMember actor) {
        eventPublisher.publishEvent(SprintUpdatedEvent.create(
                actor.getProjectKey(),
                sprint.getId(),
                sprint.getTitle(),
                changes,
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishSprintStarted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintStartedEvent.create(
                actor.getProjectKey(), sprint.getId(), sprint.getTitle(), actor.getMemberId(), actor.getDisplayName()));
    }

    public void publishSprintCompleted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintCompletedEvent.create(
                actor.getProjectKey(),
                sprint.getId(),
                sprint.getTitle(),
                sprint.getStartedAt(),
                sprint.getCompletedAt(),
                actor.getMemberId(),
                actor.getDisplayName()));
    }

    public void publishSprintCancelled(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintCancelledEvent.create(
                actor.getProjectKey(), sprint.getId(), sprint.getTitle(), actor.getMemberId(), actor.getDisplayName()));
    }

    public void publishSprintDeleted(Sprint sprint, ProjectMember actor) {
        eventPublisher.publishEvent(SprintDeletedEvent.create(
                actor.getProjectKey(), sprint.getId(), sprint.getTitle(), actor.getMemberId(), actor.getDisplayName()));
    }

    public void publishIssuesAdded(Sprint sprint, List<String> issueKeys, ProjectMember actor) {
        eventPublisher.publishEvent(SprintIssuesAddedEvent.create(
                actor.getProjectKey(), sprint.getId(), issueKeys, actor.getMemberId(), actor.getDisplayName()));
    }

    public void publishIssuesRemoved(Sprint sprint, List<String> issueKeys, ProjectMember actor) {
        eventPublisher.publishEvent(SprintIssuesRemovedEvent.create(
                actor.getProjectKey(), sprint.getId(), issueKeys, actor.getMemberId(), actor.getDisplayName()));
    }
}
