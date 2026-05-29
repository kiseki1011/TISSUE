package com.tissue.feature.comment.application.dto.response;

import com.tissue.feature.member.domain.Member;

public record CommentAuthorInfo(Long memberId, String username, String displayName) {

    public static CommentAuthorInfo from(Member member) {
        return new CommentAuthorInfo(member.getId(), member.getUsername(), member.getName());
    }
}
