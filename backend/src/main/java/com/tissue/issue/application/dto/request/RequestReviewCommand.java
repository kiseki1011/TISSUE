package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.Set;

public record RequestReviewCommand(String issueKey, Set<Long> reviewerMemberIds, ProjectMemberContext actorContext) {}
