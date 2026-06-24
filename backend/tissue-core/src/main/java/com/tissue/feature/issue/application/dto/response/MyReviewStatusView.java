package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.enums.ReviewStatus;

/**
 * A single (issue id → the calling user's review status) pair, used to enrich a
 * page of {@link IssueSummary} with the caller's own review status in one batched
 * query instead of N per-issue lookups.
 */
public record MyReviewStatusView(Long issueId, ReviewStatus status) {}
