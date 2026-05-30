package com.tissue.feature.comment.application.service;

import static com.tissue.feature.comment.domain.exception.CommentErrorCode.COMMENT_EDIT_NOT_ALLOWED;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.exception.base.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentAuthorizationService {

    public void requireCommentEditPermission(Comment comment, ProjectMember actor) {
        if (actor.getMember().hasAtLeast(SystemRole.ADMIN)) {
            return;
        }
        if (actor.isManager()) {
            return;
        }
        if (comment.isAuthor(actor.getMemberId())) {
            return;
        }
        throw new ForbiddenException(COMMENT_EDIT_NOT_ALLOWED);
    }
}
