package com.tissue.comment.application.service;

import com.tissue.comment.domain.Comment;
import com.tissue.comment.domain.exception.CommentEditNotAllowedException;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentAuthorizationService {

    public void requireCommentEditPermission(Comment comment, ProjectMemberContext actor) {
        if (actor.isWorkspaceAdmin()) {
            return;
        }
        if (isCommentAuthor(comment, actor.memberId())) {
            return;
        }
        throw new CommentEditNotAllowedException(comment.getIssue().getKey(), comment.getId(), actor.memberId());
    }

    private boolean isCommentAuthor(Comment comment, Long actorMemberId) {
        return comment.isAuthor(actorMemberId);
    }
}
