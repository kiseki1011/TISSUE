package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record SubmitReviewCommand(String issueKey, boolean approved, ProjectMemberContext actorContext) {}
