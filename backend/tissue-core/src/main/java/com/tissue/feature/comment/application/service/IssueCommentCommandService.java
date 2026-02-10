package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.in.CommentCommandUseCase;
import com.tissue.feature.comment.application.port.out.CommentRepository;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentNotFoundException;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class IssueCommentCommandService implements CommentCommandUseCase {

    private final CommentRepository commentRepository;
    private final IssueFinder issueFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final CommentAuthorizationService commentAuthorizationService;
    private final CommentEventPublisher eventPublisher;

    @Override
    public CommentCreateResponse create(String issueKey, CreateCommentCommand cmd, ProjectMemberContext actorContext) {
        Issue issue = issueFinder.getWithProjectBy(actorContext.workspaceKey(), issueKey);

        Comment parent = Optional.ofNullable(cmd.parentCommentId())
                .map(id -> commentRepository
                        .findByIssueAndId(issue, id)
                        .orElseThrow(() -> new CommentNotFoundException(issue.getKey(), id)))
                .orElse(null);

        WorkspaceMember author = workspaceMemberFinder.getBy(actorContext.workspaceKey(), actorContext.memberId());

        Comment comment = Comment.create(author, issue, cmd.content(), parent);
        commentRepository.save(comment);

        eventPublisher.publishCommentAdded(issue, comment, cmd.mentionedUsernames(), actorContext);

        return new CommentCreateResponse(issueKey, comment.getId());
    }

    @Override
    public void update(String issueKey, Long commentId, String content, ProjectMemberContext actorContext) {
        Comment comment = commentRepository
                .findWithProjectAndIssueByKeysAndId(actorContext.workspaceKey(), issueKey, commentId)
                .orElseThrow(() -> new CommentNotFoundException(issueKey, commentId));

        commentAuthorizationService.requireCommentEditPermission(comment, actorContext);

        comment.updateContent(content);

        // TODO: Publish CommentUpdatedEvent
    }

    @Override
    public void delete(String issueKey, Long commentId, ProjectMemberContext actorContext) {
        Comment comment = commentRepository
                .findWithProjectAndIssueByKeysAndId(actorContext.workspaceKey(), issueKey, commentId)
                .orElseThrow(() -> new CommentNotFoundException(issueKey, commentId));

        commentAuthorizationService.requireCommentEditPermission(comment, actorContext);

        comment.softDelete();

        // TODO: Publish CommentDeletedEvent
    }
}
