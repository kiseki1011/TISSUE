package com.tissue.feature.comment.application.port.usecase;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryUseCase {

    List<CommentDetailResponse> getIssueComments(IssueIdentifier iid, Long actorMemberId);

    Page<MyCommentResponse> getMyComments(String workspaceKey, Long actorMemberId, Pageable pageable);
}
