package com.tissue.feature.comment.application.port.usecase;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.shared.dto.IssueIdentifier;

public interface CommentCommandUseCase {
    CommentCreateResponse create(IssueIdentifier issueId, CreateCommentCommand cmd, Long memberId);

    void update(IssueIdentifier issueId, Long commentId, String content, Long memberId);

    void delete(IssueIdentifier issueId, Long commentId, Long memberId);
}
