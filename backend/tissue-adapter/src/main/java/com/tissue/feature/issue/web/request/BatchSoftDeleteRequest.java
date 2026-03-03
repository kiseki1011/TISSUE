package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.BatchSoftDeleteCommand;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record BatchSoftDeleteRequest(@NotEmpty Set<String> issueKeys) {

    public BatchSoftDeleteCommand toCommand() {
        return new BatchSoftDeleteCommand(issueKeys);
    }
}
