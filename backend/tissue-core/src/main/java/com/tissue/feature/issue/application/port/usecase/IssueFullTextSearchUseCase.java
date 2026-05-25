package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.ProjectIdentifier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IssueFullTextSearchUseCase {

    Page<IssueSummary> ftsByProject(
            ProjectIdentifier pid, IssueSearchCondition condition, Pageable pageable, Long actorMemberId);

    CursorPage<IssueSummary> ftsByProjectKeyset(
            ProjectIdentifier pid,
            IssueSearchCondition condition,
            @Nullable String cursor,
            int size,
            Long actorMemberId);
}
