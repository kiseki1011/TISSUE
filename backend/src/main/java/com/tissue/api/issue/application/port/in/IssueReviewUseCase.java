package com.tissue.api.issue.application.port.in;

import com.tissue.api.issue.application.dto.request.SubmitReviewCommand;

public interface IssueReviewUseCase {

	void submitReview(SubmitReviewCommand cmd);
}
