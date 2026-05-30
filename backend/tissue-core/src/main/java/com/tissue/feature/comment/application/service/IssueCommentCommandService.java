package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.request.UpdateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.repository.CommentRepository;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentNotFoundException;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
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
    private final ProjectAccessResolver projectAccessResolver;
    private final ProjectMemberFinder projectMemberFinder;
    private final CommentAuthorizationService commentAuthorizationService;
    private final CommentEventPublisher eventPublisher;

    @Override
    public CommentCreateResponse create(IssueIdentifier iid, CreateCommentCommand cmd, Long memberId) {
        ProjectMember actor = projectMemberFinder.getByProjectKey(iid.projectKey(), memberId);
        Issue issue = issueFinder.getWithProjectByIssueKey(iid.issueKey());

        Comment parent = Optional.ofNullable(cmd.parentCommentId())
                .map(id -> commentRepository
                        .findByIssueAndId(issue, id)
                        .orElseThrow(() -> new CommentNotFoundException(issue.getKey(), id)))
                .orElse(null);

        Comment comment = Comment.create(actor.getMember(), issue, cmd.content(), parent);
        commentRepository.save(comment);

        eventPublisher.publishCommentAdded(issue, comment, cmd.mentionedUsernames(), actor);

        return new CommentCreateResponse(iid.issueKey(), comment.getId());
    }

    @Override
    public void update(IssueIdentifier iid, Long commentId, UpdateCommentCommand cmd, Long memberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(iid.projectKey(), memberId);
        Comment comment = commentRepository
                .findWithProjectAndIssueByIssueKeyAndId(iid.issueKey(), commentId)
                .orElseThrow(() -> new CommentNotFoundException(iid.issueKey(), commentId));

        commentAuthorizationService.requireCommentEditPermission(comment, actor);

        comment.updateContent(cmd.content());

        eventPublisher.publishCommentUpdated(comment.getIssue(), comment, cmd.mentionedUsernames(), actor);
    }

    @Override
    public void delete(IssueIdentifier iid, Long commentId, Long memberId) {
        ProjectMember actor = projectAccessResolver.resolveByProjectKey(iid.projectKey(), memberId);
        Comment comment = commentRepository
                .findWithProjectAndIssueByIssueKeyAndId(iid.issueKey(), commentId)
                .orElseThrow(() -> new CommentNotFoundException(iid.issueKey(), commentId));

        commentAuthorizationService.requireCommentEditPermission(comment, actor);

        comment.softDelete();

        eventPublisher.publishCommentDeleted(comment.getIssue(), comment, actor);
    }
}
