package com.tissue.api.issue.application.port.out;

import java.util.Optional;

import com.tissue.api.issue.application.dto.response.IssueDetailDto;
import com.tissue.api.issue.domain.model.Issue;

public interface IssueQueryRepository {

	Optional<Issue> findIssue(String workspaceKey, String issueKey);

	Optional<IssueDetailDto> findDetailedIssue(String workspaceKey, String issueKey);

	// TODO: getIssueCustomFieldValues()
	// TODO: getIssues() 페이징 API
	// TODO: isStoryPointUpdatable()
}
