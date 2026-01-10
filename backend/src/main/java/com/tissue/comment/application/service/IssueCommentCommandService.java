package com.tissue.comment.application.service;

import com.tissue.comment.application.dto.in.AddCommentCommand;
import com.tissue.comment.application.dto.in.DeleteCommentCommand;
import com.tissue.comment.application.dto.in.UpdateCommentCommand;
import com.tissue.comment.application.dto.out.CommentAddResponse;
import com.tissue.comment.application.port.in.CommentCommandUseCase;
import com.tissue.comment.application.port.out.CommentRepository;
import com.tissue.comment.domain.Comment;
import com.tissue.comment.domain.exception.CommentExceptions;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.application.service.finder.ProjectMemberFinder;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
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
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public CommentAddResponse add(AddCommentCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.getBy(cmd.issueKey(), project);
        ProjectMember actor = projectMemberFinder.getBy(project, currentMemberProvider.getCurrentMemberId());

        Comment parent = Optional.ofNullable(cmd.parentCommentId())
                .map(id -> commentRepository.findById(id).orElseThrow(() -> CommentExceptions.notFound(id)))
                .orElse(null);

        Comment comment = Comment.create(issue, actor.getWorkspaceMember(), cmd.content(), parent);
        commentRepository.save(comment);

        // TODO: Publish CommentAddedEvent

        return new CommentAddResponse(cmd.workspaceKey(), cmd.issueKey(), comment.getId());
    }

    @Override
    public void update(UpdateCommentCommand cmd) {
        // TODO: consider making a CommentFinder (honestly looks kinda overkill)
        Comment comment = commentRepository
                .findById(cmd.commentId())
                .orElseThrow(() -> CommentExceptions.notFound(cmd.commentId()));
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();

        if (comment.isSoftDeleted()) {
            throw CommentExceptions.notFound(cmd.commentId());
        }
        // TODO: Make a auth check method in IssueAuthorizationService or CommentAuthorizationService
        if (!comment.getCreatedBy().equals(actorMemberId)) {
            throw CommentExceptions.notAuthor(cmd.commentId(), actorMemberId);
        }

        comment.updateContent(cmd.content());

        // TODO: Publish CommentUpdatedEvent
    }

    // TODO: allow ProjectRole.ADMIN for delete(add logic in the auth service)
    @Override
    public void delete(DeleteCommentCommand cmd) {
        Comment comment = commentRepository
                .findById(cmd.commentId())
                .orElseThrow(() -> CommentExceptions.notFound(cmd.commentId()));
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();

        if (comment.isSoftDeleted()) {
            throw CommentExceptions.notFound(cmd.commentId());
        }
        // TODO: Make a auth check method in IssueAuthorizationService or CommentAuthorizationService
        if (!comment.getCreatedBy().equals(actorMemberId)) {
            throw CommentExceptions.notAuthor(cmd.commentId(), actorMemberId);
        }

        comment.softDelete();

        // TODO: Publish CommentDeletedEvent
    }
}
