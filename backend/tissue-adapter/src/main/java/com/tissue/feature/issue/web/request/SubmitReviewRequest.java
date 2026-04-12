package com.tissue.feature.issue.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(
        @Schema(description = "`true` to approve, `false` to request changes") @NotNull
        Boolean approved) {}
