package com.tissue.comment.application.dto.out;

import com.tissue.workspace.domain.WorkspaceMember;
import org.jspecify.annotations.Nullable;

public record CommentAuthorInfo(
        Long memberId,
        String username,
        String displayName,
        @Nullable String avatarUrl // TODO: if adds avatar
        ) {

    public static CommentAuthorInfo from(WorkspaceMember wm) {
        return new CommentAuthorInfo(wm.getMember().getId(), wm.getMember().getUsername(), wm.getDisplayName(), null);
    }
}
