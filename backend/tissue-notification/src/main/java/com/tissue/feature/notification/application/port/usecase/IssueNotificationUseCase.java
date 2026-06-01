package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueDeletedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewRequestedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerAddedEvent;
import com.tissue.feature.issue.domain.event.IssueReviewerRemovedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedEvent;
import com.tissue.feature.issue.domain.event.IssueUnassignedEvent;

public interface IssueNotificationUseCase {

    void handleIssueAssigned(IssueAssignedEvent event);

    void handleIssueUnassigned(IssueUnassignedEvent event);

    void handleIssueTransitioned(IssueTransitionedEvent event);

    void handleTransitionedBySystem(IssueTransitionedBySystemEvent event);

    void handleIssueReviewerAdded(IssueReviewerAddedEvent event);

    void handleIssueReviewSubmitted(IssueReviewSubmittedEvent event);

    void handleIssueDeleted(IssueDeletedEvent event);

    void handleIssueReviewerRemoved(IssueReviewerRemovedEvent event);

    void handleIssueReviewRequested(IssueReviewRequestedEvent event);
}
