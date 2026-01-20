package com.tissue.comment.application.service.event;

import com.tissue.comment.domain.Comment;
import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCommentAdded(Issue issue, Comment comment, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueCommentAddedEvent.create(
                actor.workspaceKey(),
                actor.projectKey(),
                issue.getKey(),
                comment.getId(),
                actor.memberId(),
                actor.displayName()));
    }
}
