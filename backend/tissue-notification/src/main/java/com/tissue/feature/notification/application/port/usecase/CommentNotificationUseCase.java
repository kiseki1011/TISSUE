package com.tissue.feature.notification.application.port.usecase;

import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;

public interface CommentNotificationUseCase {

    void handleIssueCommentAdded(IssueCommentAddedEvent event);

    void handleIssueCommentUpdated(IssueCommentUpdatedEvent event);
}
