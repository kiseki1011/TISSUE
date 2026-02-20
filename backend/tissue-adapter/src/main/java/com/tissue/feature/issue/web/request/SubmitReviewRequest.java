package com.tissue.feature.issue.web.request;

import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(@NotNull Boolean approved) {}
