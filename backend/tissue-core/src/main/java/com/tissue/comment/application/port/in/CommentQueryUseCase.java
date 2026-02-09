package com.tissue.comment.application.port.in;

import com.tissue.comment.application.dto.response.CommentDetailResponse;
import com.tissue.comment.application.dto.response.MyCommentResponse;
import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentQueryUseCase {

    List<CommentDetailResponse> getIssueComments(String issueKey, ProjectMemberContext actor);

    Page<MyCommentResponse> getMyComments(Long memberId, Pageable pageable);
}
