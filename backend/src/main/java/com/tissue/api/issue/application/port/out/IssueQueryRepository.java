package com.tissue.api.issue.application.port.out;

import java.util.Optional;

import com.tissue.api.issue.adapter.in.web.dto.response.IssueDetailResponse;
import com.tissue.api.issue.domain.Issue;

public interface IssueQueryRepository {

	Optional<Issue> findIssue(String workspaceKey, String issueKey);

	Optional<IssueDetailResponse> findDetailedIssue(String workspaceKey, String issueKey);

	// TODO: getIssueCustomFieldValues(): 커스텀 필드와 값 조회
	// TODO: getIssues() 페이징 API
	// TODO: isStoryPointUpdatable()
	// TODO: getRelations
	// TODO: 참여자 관련 조회 메서드들
}
