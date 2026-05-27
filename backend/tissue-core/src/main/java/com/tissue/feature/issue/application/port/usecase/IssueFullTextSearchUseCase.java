package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.ProjectIdentifier;
import org.jspecify.annotations.Nullable;

public interface IssueFullTextSearchUseCase {

    CursorPage<IssueSummary> ftsByProjectKeyset(
            ProjectIdentifier pid,
            IssueSearchCondition condition,
            @Nullable String cursor,
            int size,
            Long actorMemberId);
}
