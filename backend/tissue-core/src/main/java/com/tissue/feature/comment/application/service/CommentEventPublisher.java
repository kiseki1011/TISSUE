package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.event.IssueCommentAddedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentDeletedEvent;
import com.tissue.feature.comment.domain.event.IssueCommentUpdatedEvent;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.ProjectMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCommentAdded(
            Issue issue, Comment comment, List<String> mentionedUsernames, ProjectMember actor) {
        eventPublisher.publishEvent(IssueCommentAddedEvent.create(
                issue.getProjectKey(),
                issue.getKey(),
                comment.getId(),
                comment.getContent(),
                mentionedUsernames,
                actor.getMember().getId(),
                actor.getDisplayName()));
    }

    public void publishCommentUpdated(
            Issue issue, Comment comment, List<String> mentionedUsernames, ProjectMember actor) {
        eventPublisher.publishEvent(IssueCommentUpdatedEvent.create(
                issue.getProjectKey(),
                issue.getKey(),
                comment.getId(),
                comment.getContent(),
                mentionedUsernames,
                actor.getMember().getId(),
                actor.getDisplayName()));
    }

    public void publishCommentDeleted(Issue issue, Comment comment, ProjectMember actor) {
        eventPublisher.publishEvent(IssueCommentDeletedEvent.create(
                issue.getProjectKey(),
                issue.getKey(),
                comment.getId(),
                actor.getMember().getId(),
                actor.getDisplayName()));
    }
}
