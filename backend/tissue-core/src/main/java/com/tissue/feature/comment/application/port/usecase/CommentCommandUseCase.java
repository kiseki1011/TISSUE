package com.tissue.feature.comment.application.port.usecase;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.request.UpdateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.shared.dto.IssueIdentifier;

public interface CommentCommandUseCase {
    CommentCreateResponse create(IssueIdentifier iid, CreateCommentCommand cmd, Long memberId);

    /**
     * Stores the feedback body of a submitted review as a comment stamped with the verdict. Called by
     * the review feature, not exposed over the web API: an ordinary comment must never be able to claim
     * a verdict it was not submitted with.
     */
    CommentCreateResponse createReview(IssueIdentifier iid, String content, ReviewStatus reviewStatus, Long memberId);

    void update(IssueIdentifier iid, Long commentId, UpdateCommentCommand cmd, Long memberId);

    void delete(IssueIdentifier iid, Long commentId, Long memberId);
}
