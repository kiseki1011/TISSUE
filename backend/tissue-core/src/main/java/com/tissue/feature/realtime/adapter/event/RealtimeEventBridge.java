package com.tissue.feature.realtime.adapter.event;

import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueCreatedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueFieldsUpdatedEvent;
import com.tissue.feature.issue.domain.event.IssueRelationAddedEvent;
import com.tissue.feature.issue.domain.event.IssueRelationRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;
import com.tissue.feature.realtime.application.RealtimeBroadcaster;
import java.time.Instant;
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
 * Bridges issue domain events (lifecycle, reviewers, relations) onto the realtime SSE stream,
 * delivering each to the emitters of the project's connected members.
 *
 * <p>Runs after the transaction is committed ({@link TransactionPhase#AFTER_COMMIT}) on a separate
 * thread, so SSE updates never block the request and are sent only after changes are committed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeEventBridge {

    private static final String ISSUE_CATEGORY = "issue";

    private final RealtimeBroadcaster broadcaster;

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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewerAdded(IssueReviewerAddedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_REVIEWER_ADDED",
                Map.of("reviewerMemberId", event.reviewerMemberId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewerRemoved(IssueReviewerRemovedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_REVIEWER_REMOVED",
                Map.of("removedReviewerMemberId", event.removedReviewerMemberId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewSubmitted(IssueReviewSubmittedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_REVIEW_SUBMITTED",
                Map.of("reviewStatus", event.reviewStatus().name()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewRequested(IssueReviewRequestedEvent event) {
        broadcastIssue(
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_REVIEW_REQUESTED",
                Map.of("reviewerCount", event.reviewerCount()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRelationAdded(IssueRelationAddedEvent event) {
        broadcastRelation(
                event.eventId(),
                event.sourceProjectKey(),
                event.sourceIssueKey(),
                event.targetProjectKey(),
                event.targetIssueKey(),
                event.relationId(),
                event.relationType().name(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_RELATION_ADDED");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRelationRemoved(IssueRelationRemovedEvent event) {
        broadcastRelation(
                event.eventId(),
                event.sourceProjectKey(),
                event.sourceIssueKey(),
                event.targetProjectKey(),
                event.targetIssueKey(),
                event.relationId(),
                event.relationType().name(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_RELATION_REMOVED");
    }

    private void broadcastRelation(
            UUID eventId,
            String sourceProjectKey,
            String sourceIssueKey,
            String targetProjectKey,
            String targetIssueKey,
            Long relationId,
            String relationType,
            Long actorMemberId,
            Instant occurredAt,
            String type) {
        Map<String, Object> data = Map.of("relationId", relationId, "relationType", relationType);
        broadcastIssue(eventId, sourceProjectKey, sourceIssueKey, actorMemberId, occurredAt, type, data);
        // A relation touches both issues' detail; notify the target's project too.
        if (!targetIssueKey.equals(sourceIssueKey)) {
            broadcastIssue(eventId, targetProjectKey, targetIssueKey, actorMemberId, occurredAt, type, data);
        }
    }

    private void broadcastIssue(
            UUID eventId,
            String projectKey,
            String issueKey,
            Long actorMemberId,
            Instant occurredAt,
            String type,
            Map<String, Object> data) {
        broadcaster.broadcast(ISSUE_CATEGORY, eventId, projectKey, issueKey, actorMemberId, occurredAt, type, data);
    }
}
