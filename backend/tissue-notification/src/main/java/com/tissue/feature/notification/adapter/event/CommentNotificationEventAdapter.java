package com.tissue.feature.notification.adapter.event;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;
import com.tissue.feature.notification.application.port.usecase.CommentNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentNotificationEventAdapter {

    private final CommentNotificationUseCase useCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentAdded(IssueCommentAddedEvent event) {
        useCase.handleIssueCommentAdded(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueCommentUpdated(IssueCommentUpdatedEvent event) {
        useCase.handleIssueCommentUpdated(event);
    }
}
