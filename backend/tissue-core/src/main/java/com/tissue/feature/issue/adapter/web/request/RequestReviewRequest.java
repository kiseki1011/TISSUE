package com.tissue.feature.issue.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RequestReviewRequest(
        @Schema(description = "Project member IDs to request review from") @NotNull @NotEmpty @Size(max = 10)
        Set<Long> reviewerMemberIds) {}
