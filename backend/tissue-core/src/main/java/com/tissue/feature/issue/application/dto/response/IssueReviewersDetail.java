package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.application.dto.response.info.ReviewerInfo;
import com.tissue.feature.issue.domain.IssueReviewer;
import java.util.List;

public record IssueReviewersDetail(List<ReviewerInfo> reviewers, int totalCount) {
    public static IssueReviewersDetail from(List<IssueReviewer> reviewers) {
        List<ReviewerInfo> infos = reviewers.stream().map(ReviewerInfo::from).toList();
        return new IssueReviewersDetail(infos, infos.size());
    }
}
