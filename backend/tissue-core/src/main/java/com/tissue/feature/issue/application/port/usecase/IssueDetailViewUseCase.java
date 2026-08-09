package com.tissue.feature.issue.application.port.usecase;

import com.tissue.feature.issue.application.dto.request.IssueDetailSection;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;

public interface IssueDetailViewUseCase {

    /**
     * Everything an issue detail screen needs in one response. Sections outside {@code sections} are not
     * queried and come back empty.
     *
     * @param commentSize how many root comments to embed; ignored unless comments were requested
     */
    IssueDetailView getDetailView(
            IssueIdentifier iid, Set<IssueDetailSection> sections, int commentSize, Long actorMemberId);
}
