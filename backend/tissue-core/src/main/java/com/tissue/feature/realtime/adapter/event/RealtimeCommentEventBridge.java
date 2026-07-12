package com.tissue.feature.realtime.adapter.event;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentDeletedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;
import com.tissue.feature.realtime.application.RealtimeBroadcaster;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges issue-comment domain events onto the realtime SSE stream under the "issue" category.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeCommentEventBridge {

    private static final String ISSUE_CATEGORY = "issue";

    private final RealtimeBroadcaster broadcaster;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(IssueCommentAddedEvent event) {
        broadcaster.broadcast(
                ISSUE_CATEGORY,
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_COMMENT_ADDED",
                Map.of("commentId", event.commentId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentUpdated(IssueCommentUpdatedEvent event) {
        broadcaster.broadcast(
                ISSUE_CATEGORY,
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_COMMENT_UPDATED",
                Map.of("commentId", event.commentId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentDeleted(IssueCommentDeletedEvent event) {
        broadcaster.broadcast(
                ISSUE_CATEGORY,
                event.eventId(),
                event.projectKey(),
                event.issueKey(),
                event.actorMemberId(),
                event.occurredAt(),
                "ISSUE_COMMENT_DELETED",
                Map.of("commentId", event.commentId()));
    }
}
