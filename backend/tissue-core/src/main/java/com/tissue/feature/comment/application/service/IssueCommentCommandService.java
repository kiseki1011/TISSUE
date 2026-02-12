package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.repository.CommentRepository;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentNotFoundException;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.shared.dto.IssueIdentifier;
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
    public CommentCreateResponse create(IssueIdentifier issueId, CreateCommentCommand cmd, Long memberId) {
        Issue issue = issueFinder.getWithProjectBy(issueId.workspaceKey(), issueId.issueKey());

        Comment parent = Optional.ofNullable(cmd.parentCommentId())
                .map(id -> commentRepository
                        .findByIssueAndId(issue, id)
                        .orElseThrow(() -> new CommentNotFoundException(issue.getKey(), id)))
                .orElse(null);

        WorkspaceMember author = workspaceMemberFinder.getBy(issueId.workspaceKey(), memberId);

        Comment comment = Comment.create(author, issue, cmd.content(), parent);
        commentRepository.save(comment);

        eventPublisher.publishCommentAdded(issue, comment, cmd.mentionedUsernames(), author);

        return new CommentCreateResponse(issueId.issueKey(), comment.getId());
    }

    @Override
    public void update(IssueIdentifier issueId, Long commentId, String content, Long memberId) {
        Comment comment = commentRepository
                .findWithProjectAndIssueByKeysAndId(issueId.workspaceKey(), issueId.issueKey(), commentId)
                .orElseThrow(() -> new CommentNotFoundException(issueId.issueKey(), commentId));

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueId.workspaceKey(), memberId);
        commentAuthorizationService.requireCommentEditPermission(comment, actor);

        comment.updateContent(content);

        // TODO: Publish CommentUpdatedEvent
    }

    @Override
    public void delete(IssueIdentifier issueId, Long commentId, Long memberId) {
        Comment comment = commentRepository
                .findWithProjectAndIssueByKeysAndId(issueId.workspaceKey(), issueId.issueKey(), commentId)
                .orElseThrow(() -> new CommentNotFoundException(issueId.issueKey(), commentId));

        WorkspaceMember actor = workspaceMemberFinder.getBy(issueId.workspaceKey(), memberId);
        commentAuthorizationService.requireCommentEditPermission(comment, actor);

        comment.softDelete();

        // TODO: Publish CommentDeletedEvent
    }
}
