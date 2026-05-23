package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.enums.ReviewStatus;

public record ReviewerInfo(ParticipantInfo participant, ReviewStatus status) {
    public static ReviewerInfo from(IssueReviewer reviewer) {
        return new ReviewerInfo(ParticipantInfo.from(reviewer.getReviewer()), reviewer.getStatus());
    }
}
