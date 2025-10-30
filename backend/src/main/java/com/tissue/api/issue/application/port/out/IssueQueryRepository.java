package com.tissue.api.issue.application.port.out;

import java.util.List;
import java.util.Optional;

import com.tissue.api.issue.domain.Issue;

public interface IssueQueryRepository {

	Optional<Issue> findWithBasicInfo(String workspaceKey, String issueKey);

	Optional<Issue> findWithDetail(String workspaceKey, String issueKey);

	Optional<Issue> findWithParent(String workspaceKey, String issueKey);

	List<Issue> findChildren(String workspaceKey, String issueKey);
}
