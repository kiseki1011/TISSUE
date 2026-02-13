package com.tissue.feature.comment.application.service;

import static com.tissue.feature.comment.domain.exception.CommentErrorCode.COMMENT_EDIT_NOT_ALLOWED;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.ForbiddenException;
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
        throw new ForbiddenException(COMMENT_EDIT_NOT_ALLOWED);
    }

    private boolean isCommentAuthor(Comment comment, Long actorMemberId) {
        return comment.isAuthor(actorMemberId);
    }
}
