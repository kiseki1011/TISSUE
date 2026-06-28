package com.tissue.feature.comment.application.port.usecase;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.shared.dto.IssueIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryUseCase {

    /**
     * Returns root comments on an issue, paginated. Each root carries its replies inline
     * (max-depth is 1 by domain rules, so no deeper nesting needs paging).
     */
    Page<CommentDetailResponse> getIssueComments(IssueIdentifier iid, Pageable pageable, Long actorMemberId);

    Page<MyCommentResponse> getMyComments(Long actorMemberId, Pageable pageable);
}
