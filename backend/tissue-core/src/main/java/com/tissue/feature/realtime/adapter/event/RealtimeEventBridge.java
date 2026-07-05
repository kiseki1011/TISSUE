package com.tissue.feature.realtime.adapter.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.realtime.application.SseEmitterRegistry;
import com.tissue.feature.realtime.application.dto.RealtimeMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges issue domain events onto the realtime SSE stream, delivering each event to the emitters
 * of the connected members of the project.
 *
 * <p>Runs after the transaction is committed ({@link TransactionPhase#AFTER_COMMIT}) on a separate
 * thread. This ensures SSE updates never block the request and are sent only after changes are
 * successfully committed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeEventBridge {

    private static final String ISSUE_CATEGORY = "issue";

    private final SseEmitterRegistry registry;
    private final ProjectMemberQueryRepository projectMemberQueryRepository;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCreated(IssueCreatedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_CREATED",
                Map.of());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueTransitioned(IssueTransitionedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_TRANSITIONED",
                Map.of("newStateId", event.newStateId(), "newStateName", event.newStateName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueAssigned(IssueAssignedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_ASSIGNED",
                Map.of(
                        "assigneeMemberId",
                        event.assigneeMemberId(),
                        "assigneeDisplayName",
                        event.assigneeDisplayName()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueUnassigned(IssueUnassignedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_UNASSIGNED",
                Map.of("removedAssigneeMemberId", event.removedAssigneeMemberId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueFieldsUpdated(IssueFieldsUpdatedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_FIELDS_UPDATED",
                Map.of("fields", List.copyOf(event.changes().keySet())));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueDeleted(IssueDeletedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_DELETED",
                Map.of());
    }

    private void broadcastIssue(
            UUID eventId,
            String projectKey,
            String issueKey,
            Long actorMemberId,
            Instant occurredAt,
            String type,
            Map<String, Object> data) {
        Set<Long> memberIds = projectMemberQueryRepository.findMemberIdsByProjectKey(projectKey);
        if (memberIds.isEmpty()) {
            return;
        }
        RealtimeMessage message = new RealtimeMessage(type, projectKey, issueKey, actorMemberId, occurredAt, data);
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.warn("Realtime: failed to serialize {} for {}", type, issueKey, e);
            return;
        }
        registry.send(memberIds, ISSUE_CATEGORY, eventId.toString(), json);
    }
}
