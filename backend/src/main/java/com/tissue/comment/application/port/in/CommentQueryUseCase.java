package com.tissue.comment.application.port.in;

import com.tissue.comment.application.dto.out.CommentDetailResponse;
import com.tissue.comment.application.dto.out.MyCommentResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryUseCase {

    List<CommentDetailResponse> getIssueComments(String workspaceKey, String projectKey, String issueKey);

    Page<MyCommentResponse> getMyComments(Long memberId, Pageable pageable);
}
