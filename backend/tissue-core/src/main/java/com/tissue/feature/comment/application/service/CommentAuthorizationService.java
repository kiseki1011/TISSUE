package com.tissue.feature.comment.application.service;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.comment.domain.exception.CommentEditNotAllowedException;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
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
