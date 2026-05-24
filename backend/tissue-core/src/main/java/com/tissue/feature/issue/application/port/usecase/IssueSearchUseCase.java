package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssueSearchUseCase {

    Page<IssueSummary> searchByProject(
            ProjectIdentifier pid, IssueSearchCondition condition, Pageable pageable, Long actorMemberId);

    Page<IssueSummary> searchByWorkspace(
            String workspaceKey, IssueSearchCondition condition, Pageable pageable, Long actorMemberId);
}
