package com.tissue.comment.application.service;

import com.tissue.comment.application.dto.request.AddCommentCommand;
import com.tissue.comment.application.dto.request.DeleteCommentCommand;
import com.tissue.comment.application.dto.request.UpdateCommentCommand;
import com.tissue.comment.application.dto.response.CommentAddResponse;
import com.tissue.comment.application.port.in.CommentCommandUseCase;
import com.tissue.comment.application.port.out.CommentRepository;
import com.tissue.comment.domain.Comment;
import com.tissue.comment.domain.exception.CommentNotFoundException;
import com.tissue.issue.application.service.authorization.IssueAuthorizationService;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceMember;
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
    private final IssueAuthorizationService issueAuthorizationService;
    private final CommentEventPublisher eventPublisher;

    @Override
    public CommentAddResponse add(AddCommentCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Issue issue = issueFinder.getBy(actorContext.workspaceKey(), cmd.issueKey());

        Comment parent = Optional.ofNullable(cmd.parentCommentId())
                .map(id -> commentRepository
                        .findByIdAndIssue_Key(id, issue.getKey())
                        .orElseThrow(() -> new CommentNotFoundException(id, cmd.issueKey())))
                .orElse(null);

        WorkspaceMember author = workspaceMemberFinder.getActive(actorContext.memberId(), actorContext.workspaceKey());

        Comment comment = Comment.create(author, issue, cmd.content(), parent);
        commentRepository.save(comment);

        eventPublisher.publishCommentAdded(issue, comment, cmd.mentionedUsernames(), actorContext);

        return new CommentAddResponse(actorContext.workspaceKey(), cmd.issueKey(), comment.getId());
    }

    @Override
    public void update(UpdateCommentCommand cmd) {
        Comment comment = commentRepository
                .findByIdAndIssue_Key(cmd.commentId(), cmd.issueKey())
                .orElseThrow(() -> new CommentNotFoundException(cmd.commentId(), cmd.issueKey()));

        issueAuthorizationService.requireCommentEditPermission(comment, cmd.actor());

        comment.updateContent(cmd.content());

        // TODO: Publish CommentUpdatedEvent
    }

    @Override
    public void delete(DeleteCommentCommand cmd) {
        Comment comment = commentRepository
                .findByIdAndIssue_Key(cmd.commentId(), cmd.issueKey())
                .orElseThrow(() -> new CommentNotFoundException(cmd.commentId(), cmd.issueKey()));

        issueAuthorizationService.requireCommentEditPermission(comment, cmd.actor());

        comment.softDelete();

        // TODO: Publish CommentDeletedEvent
    }
}
