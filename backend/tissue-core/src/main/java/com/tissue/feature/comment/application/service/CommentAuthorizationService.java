package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentEditNotAllowedException;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentAuthorizationService {

    public void requireCommentEditPermission(Comment comment, WorkspaceMember actor) {
        if (actor.getRole().isEqualOrHigherThan(WorkspaceRole.ADMIN)) {
            return;
        }
        if (isCommentAuthor(comment, actor.getMember().getId())) {
            return;
        }
        throw new CommentEditNotAllowedException(
                comment.getIssue().getKey(), comment.getId(), actor.getMember().getId());
    }

    private boolean isCommentAuthor(Comment comment, Long actorMemberId) {
        return comment.isAuthor(actorMemberId);
    }
}
