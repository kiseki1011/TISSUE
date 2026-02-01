package com.tissue.issue.adapter.web.request;

import com.tissue.issue.application.dto.request.SubmitReviewCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(@NotNull Boolean approved) {

    public SubmitReviewCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return new SubmitReviewCommand(issueKey, approved, actorContext);
    }
}
