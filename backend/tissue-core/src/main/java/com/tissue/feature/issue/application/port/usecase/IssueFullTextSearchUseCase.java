package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import org.springframework.data.domain.Page;

public interface IssueFullTextSearchUseCase {

    Page<IssueSummary> ftsByProjectRanked(
            ProjectIdentifier pid, IssueSearchCondition condition, int page, int size, Long actorMemberId);
}
