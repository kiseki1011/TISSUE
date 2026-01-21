package com.tissue.issue.application.port.in;

import com.tissue.issue.application.dto.request.RequestReviewCommand;
import com.tissue.issue.application.dto.request.SubmitReviewCommand;

public interface IssueReviewUseCase {

    void submitReview(SubmitReviewCommand cmd);

    void requestReview(RequestReviewCommand cmd);
}
