package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.SubmitReviewCommand;

public interface IssueReviewUseCase {

    void submitReview(SubmitReviewCommand cmd);

    // TODO: requestReview
    //  해당 특정 이슈에 대한 모든 리뷰어에게 리뷰를 해달라고 알림?
}
