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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueCommentCommandService implements CommentCommandUseCase {

    private final CommentRepository commentRepository;
    private final IssueFinder issueFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;

    @Transactional
    @Override
    public CommentAddResponse add(AddCommentCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Issue issue = issueFinder.findBy(cmd.issueKey(), project);
        ProjectMember actor = projectMemberFinder.findBy(project, cmd.actorMemberId());

        Comment parent = null;
        if (cmd.parentCommentId() != null) {
            parent = commentRepository
                    .findById(cmd.parentCommentId())
                    .orElseThrow(() -> CommentExceptions.notFound(cmd.parentCommentId()));
        }

        // Use WorkspaceMember as the author
        Comment comment = Comment.create(issue, actor.getWorkspaceMember(), cmd.content(), parent);
        commentRepository.save(comment);

        // TODO: Publish CommentAddedEvent

        return new CommentAddResponse(cmd.workspaceKey(), cmd.issueKey(), comment.getId());
    }

    @Transactional
    @Override
    public void update(UpdateCommentCommand cmd) {
        Comment comment = commentRepository
                .findById(cmd.commentId())
                .orElseThrow(() -> CommentExceptions.notFound(cmd.commentId()));

        if (comment.isSoftDeleted()) {
            throw CommentExceptions.notFound(cmd.commentId());
        }

        if (!comment.getCreatedBy().equals(cmd.actorMemberId())) {
            throw CommentExceptions.notAuthor(cmd.commentId(), cmd.actorMemberId());
        }

        comment.updateContent(cmd.content());

        // TODO: Publish CommentUpdatedEvent
    }

    // TODO: should i allow ProjectRole.ADMIN for delete?
    @Transactional
    @Override
    public void delete(DeleteCommentCommand cmd) {
        Comment comment = commentRepository
                .findById(cmd.commentId())
                .orElseThrow(() -> CommentExceptions.notFound(cmd.commentId()));

        if (comment.isSoftDeleted()) {
            throw CommentExceptions.notFound(cmd.commentId());
        }

        if (!comment.getCreatedBy().equals(cmd.actorMemberId())) {
            throw CommentExceptions.notAuthor(cmd.commentId(), cmd.actorMemberId());
        }

        comment.softDelete();

        // TODO: Publish CommentDeletedEvent
    }
}
