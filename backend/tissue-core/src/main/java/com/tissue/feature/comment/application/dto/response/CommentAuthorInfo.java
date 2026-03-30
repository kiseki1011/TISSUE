package com.tissue.feature.comment.application.dto.response;

import com.tissue.feature.workspace.domain.WorkspaceMember;

public record CommentAuthorInfo(Long memberId, String username, String displayName) {

    public static CommentAuthorInfo from(WorkspaceMember wm) {
        return new CommentAuthorInfo(wm.getMember().getId(), wm.getMember().getUsername(), wm.getDisplayName());
    }
}
