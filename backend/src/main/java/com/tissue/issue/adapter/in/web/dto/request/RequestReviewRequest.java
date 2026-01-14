package com.tissue.issue.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RequestReviewRequest(@NotNull @NotEmpty Set<Long> reviewerMemberIds) {}
