package com.tissue.comment.application.service.event;

import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.ProjectMember;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCommentAdded(Issue issue, Long commentId, ProjectMember actor) {
        eventPublisher.publishEvent(IssueCommentAddedEvent.create(
                issue.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                issue.getId(),
                commentId,
                actor.getMemberId(),
                actor.getDisplayName()));
    }
}
