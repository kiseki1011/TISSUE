package com.tissue.feature.comment.application.port.usecase;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.shared.dto.IssueIdentifier;

public interface CommentCommandUseCase {
    CommentCreateResponse create(IssueIdentifier issueIdentifier, CreateCommentCommand cmd, Long memberId);

    void update(IssueIdentifier issueIdentifier, Long commentId, String content, Long memberId);

    void delete(IssueIdentifier issueIdentifier, Long commentId, Long memberId);
}
