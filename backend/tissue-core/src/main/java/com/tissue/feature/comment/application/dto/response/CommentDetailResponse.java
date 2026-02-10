package com.tissue.feature.comment.application.dto.response;

import com.tissue.feature.comment.domain.Comment;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CommentDetailResponse(
        Long commentId,
        @Nullable String content,
        boolean isEdited,
        boolean isDeleted,
        Instant createdAt,
        Instant lastUpdatedAt,
        CommentAuthorInfo author,
        List<CommentDetailResponse> replies) {

    public static CommentDetailResponse from(Comment comment, List<CommentDetailResponse> replies) {
        boolean deleted = comment.isSoftDeleted();

        return new CommentDetailResponse(
                comment.getId(),
                deleted ? null : comment.getContent(),
                comment.isEdited(),
                deleted,
                comment.getCreatedAt(),
                comment.getLastModifiedAt(),
                CommentAuthorInfo.from(comment.getAuthor()),
                replies != null ? replies : new ArrayList<>());
    }
}
