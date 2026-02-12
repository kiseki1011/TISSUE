package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCommentAdded(
            Issue issue, Comment comment, List<String> mentionedUsernames, WorkspaceMember actor) {
        eventPublisher.publishEvent(IssueCommentAddedEvent.create(
                actor.getWorkspaceKey(),
                issue.getProjectKey(),
                issue.getKey(),
                comment.getId(),
                comment.getContent(),
                mentionedUsernames,
                actor.getMember().getId(),
                actor.getDisplayName()));
    }
}
