package com.tissue.issue.web.request;

import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(@NotNull Boolean approved) {}
