package com.tissue.feature.comment.application.port.usecase;

import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.comment.application.dto.response.MyCommentResponse;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryUseCase {

    List<CommentDetailResponse> getIssueComments(String issueKey, ProjectMemberContext actor);

    Page<MyCommentResponse> getMyComments(Long memberId, Pageable pageable);
}
