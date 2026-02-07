package com.tissue.comment.application.service;

import com.tissue.comment.domain.Comment;
import com.tissue.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCommentAdded(
            Issue issue, Comment comment, List<String> mentionedUsernames, ProjectMemberContext actor) {
        eventPublisher.publishEvent(IssueCommentAddedEvent.create(
                actor.workspaceKey(),
                actor.projectKey(),
                issue.getKey(),
                comment.getId(),
                comment.getContent(),
                mentionedUsernames,
                actor.memberId(),
                actor.displayName()));
    }
}
