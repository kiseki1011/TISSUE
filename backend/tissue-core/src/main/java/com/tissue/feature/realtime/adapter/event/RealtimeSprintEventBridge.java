package com.tissue.feature.realtime.adapter.event;

import com.tissue.feature.realtime.application.RealtimeBroadcaster;
import com.tissue.feature.sprint.domain.event.SprintCancelledEvent;
import com.tissue.feature.sprint.domain.event.SprintCompletedEvent;
import com.tissue.feature.sprint.domain.event.SprintCreatedEvent;
import com.tissue.feature.sprint.domain.event.SprintDeletedEvent;
import com.tissue.feature.sprint.domain.event.SprintIssuesAddedEvent;
import com.tissue.feature.sprint.domain.event.SprintIssuesRemovedEvent;
import com.tissue.feature.sprint.domain.event.SprintStartedEvent;
import com.tissue.feature.sprint.domain.event.SprintUpdatedEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges sprint domain events onto the realtime SSE stream under the "sprint" category.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSprintEventBridge {

    private static final String SPRINT_CATEGORY = "sprint";

    private final RealtimeBroadcaster broadcaster;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintCreated(SprintCreatedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_CREATED",
                Map.of("sprintTitle", event.sprintTitle()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintUpdated(SprintUpdatedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_UPDATED",
                Map.of("fields", List.copyOf(event.changes().keySet())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintStarted(SprintStartedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_STARTED",
                Map.of());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintCompleted(SprintCompletedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_COMPLETED",
                Map.of());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintCancelled(SprintCancelledEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_CANCELLED",
                Map.of());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintDeleted(SprintDeletedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_DELETED",
                Map.of());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintIssuesAdded(SprintIssuesAddedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_ISSUES_ADDED",
                Map.of("issueKeys", event.issueKeys()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSprintIssuesRemoved(SprintIssuesRemovedEvent event) {
        broadcastSprint(
                event.eventId(),
                event.projectKey(),
                event.sprintId(),
                event.actorMemberId(),
                event.occurredAt(),
                "SPRINT_ISSUES_REMOVED",
                Map.of("issueKeys", event.issueKeys()));
    }

    private void broadcastSprint(
            UUID eventId,
            String projectKey,
            Long sprintId,
            Long actorMemberId,
            Instant occurredAt,
            String type,
            Map<String, Object> extra) {
        Map<String, Object> data = new HashMap<>(extra);
        data.put("sprintId", sprintId);
        broadcaster.broadcast(SPRINT_CATEGORY, eventId, projectKey, null, actorMemberId, occurredAt, type, data);
    }
}
