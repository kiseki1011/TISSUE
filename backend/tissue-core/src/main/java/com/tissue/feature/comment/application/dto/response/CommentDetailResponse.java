package com.tissue.feature.comment.application.dto.response;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(
        description = "A comment on an issue with author info and nested replies. "
                + "Only deleted comments contents are `null` to preserve structure. "
                + "`reviewStatus` is set only when the comment is the feedback body of a submitted review, "
                + "and holds the verdict it was submitted with.",
        example = """
        {
          "commentId": 1,
          "content": "We should refactor this module first.",
          "isEdited": false,
          "isDeleted": false,
          "reviewStatus": null,
          "createdAt": "2026-01-06T09:00:00Z",
          "lastUpdatedAt": "2026-01-06T09:00:00Z",
          "author": {
            "memberId": 123,
            "username": "gildong",
            "displayName": "Gildong"
          },
          "replies": [
            {
              "commentId": 2,
              "content": "Sure, I'll create a subtask for it.",
              "isEdited": true,
              "isDeleted": false,
              "createdAt": "2026-01-06T09:15:00Z",
              "lastUpdatedAt": "2026-01-06T09:20:00Z",
              "author": {
                "memberId": 124,
                "username": "bob",
                "displayName": "Bob"
              },
              "replies": []
            }
          ]
        }""")
public record CommentDetailResponse(
        Long commentId,
        @Nullable String content,
        boolean isEdited,
        boolean isDeleted,
        @Nullable ReviewStatus reviewStatus,
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
                comment.getReviewStatus(),
                comment.getCreatedAt(),
                comment.getLastModifiedAt(),
                CommentAuthorInfo.from(comment.getAuthor()),
                replies != null ? replies : new ArrayList<>());
    }
}
