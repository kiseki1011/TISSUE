package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssueSearchRepository {

    Page<Issue> searchByProject(Project project, IssueSearchCondition condition, Pageable pageable);

    /**
     * Workspace scoped search restricted to issues in projects the actor belongs to.
     * Project membership check is enforced inside the repository via a subquery so a
     * workspace member cannot leak issues from projects they did not join.
     */
    Page<Issue> searchByWorkspace(
            String workspaceKey, IssueSearchCondition condition, Pageable pageable, Long actorMemberId);
}
