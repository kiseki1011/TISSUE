package com.tissue.feature.issue.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RequestReviewRequest(
        @NotNull @NotEmpty @Size(max = 10) Set<Long> reviewerMemberIds) {}
