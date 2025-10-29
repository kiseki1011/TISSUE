package com.tissue.api.issue.application.port.out;

import java.util.Optional;

import com.tissue.api.issue.domain.Issue;

public interface IssueQueryRepository {

	Optional<Issue> findWithBasicInfo(String workspaceKey, String issueKey);

	Optional<Issue> findWithDetail(String workspaceKey, String issueKey);

	// TODO: findWithCustomFieldValues(): 커스텀 필드와 값 조회
	// TODO: findIssues() 페이징 API
	// TODO: isStoryPointUpdatable()
	// TODO: findRelations()
	// TODO: findSubscribers()
}
