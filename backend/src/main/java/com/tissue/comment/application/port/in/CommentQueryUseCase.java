package com.tissue.comment.application.port.in;

import java.util.List;

import com.tissue.comment.application.dto.out.CommentDetailResponse;

public interface CommentQueryUseCase {

	List<CommentDetailResponse> getIssueComments(String workspaceKey, String projectKey, String issueKey);

	// TODO: getMyComments (Pagination)
}
