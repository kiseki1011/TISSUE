package com.tissue.feature.issue.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RequestReviewRequest(@NotNull @NotEmpty Set<Long> reviewerMemberIds) {}
