package com.tissue.feature.issue.adapter.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record SubmitReviewRequest(
        @Schema(description = "`true` to approve, `false` to request changes") @NotNull
        Boolean approved,

        @Schema(
                description = "Optional feedback body. When present it is stored as a comment on the issue, "
                        + "stamped with the verdict this review was submitted with.")
        @Nullable
        @Size(max = 10000)
        String comment) {}
