package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.feature.issue.domain.IssueReviewer;
import java.util.List;

public record IssueReviewersDetail(List<ParticipantInfo> reviewers, int totalCount) {

    public static IssueReviewersDetail from(List<IssueReviewer> reviewers) {
        return new IssueReviewersDetail(
                reviewers.stream()
                        .map(IssueReviewer::getReviewer)
                        .map(ParticipantInfo::from)
                        .toList(),
                reviewers.size());
    }
}
