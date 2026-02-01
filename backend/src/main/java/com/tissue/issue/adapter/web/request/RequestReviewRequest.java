package com.tissue.issue.adapter.web.request;

import com.tissue.issue.application.dto.request.RequestReviewCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RequestReviewRequest(@NotNull @NotEmpty Set<Long> reviewerMemberIds) {

    public RequestReviewCommand toCommand(String issueKey, ProjectMemberContext actorContext) {
        return new RequestReviewCommand(issueKey, reviewerMemberIds, actorContext);
    }
}
