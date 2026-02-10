package com.tissue.feature.comment.application.dto.response;

import com.tissue.feature.workspace.domain.WorkspaceMember;

// TODO: use a common and consistent response structure
public record CommentAuthorInfo(Long memberId, String username, String displayName) {
    public static CommentAuthorInfo from(WorkspaceMember wm) {
        return new CommentAuthorInfo(wm.getMember().getId(), wm.getMember().getUsername(), wm.getDisplayName());
    }
}
