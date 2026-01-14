package com.tissue.issue.application.dto.request;

import java.util.Set;
import lombok.Builder;

@Builder
public record RequestReviewCommand(
        String workspaceKey, String projectKey, String issueKey, Set<Long> reviewerMemberIds, Long actorMemberId) {}
