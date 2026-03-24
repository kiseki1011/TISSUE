package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.BatchSoftDeleteCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record BatchSoftDeleteRequest(
        @NotEmpty @Size(max = 100) Set<String> issueKeys) {

    public BatchSoftDeleteCommand toCommand() {
        return new BatchSoftDeleteCommand(issueKeys);
    }
}
