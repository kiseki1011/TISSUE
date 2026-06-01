package com.tissue.feature.notification.adapter.event;

import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;
import com.tissue.feature.notification.application.port.usecase.IssueNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class IssueNotificationEventAdapter {

    private final IssueNotificationUseCase useCase;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueAssigned(IssueAssignedEvent event) {
        useCase.handleIssueAssigned(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueUnassigned(IssueUnassignedEvent event) {
        useCase.handleIssueUnassigned(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueTransitioned(IssueTransitionedEvent event) {
        useCase.handleIssueTransitioned(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTransitionedBySystem(IssueTransitionedBySystemEvent event) {
        useCase.handleTransitionedBySystem(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerAdded(IssueReviewerAddedEvent event) {
        useCase.handleIssueReviewerAdded(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event) {
        useCase.handleIssueReviewSubmitted(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueDeleted(IssueDeletedEvent event) {
        useCase.handleIssueDeleted(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event) {
        useCase.handleIssueReviewerRemoved(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIssueReviewRequested(IssueReviewRequestedEvent event) {
        useCase.handleIssueReviewRequested(event);
    }
}
