package com.tissue.shared.dto;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record BatchOperationResponse(int totalCount, int successCount, int failCount, List<BatchFailure> failures) {

    public record BatchFailure(String key, @Nullable String message) {}

    public static BatchOperationResponse of(int totalCount, List<BatchFailure> failures) {
        int failCount = failures.size();
        return new BatchOperationResponse(totalCount, totalCount - failCount, failCount, failures);
    }
}
