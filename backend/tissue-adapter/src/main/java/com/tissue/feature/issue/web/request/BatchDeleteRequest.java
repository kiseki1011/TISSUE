package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.BatchDeleteCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record BatchDeleteRequest(@NotEmpty @Size(max = 100) Set<String> issueKeys) {

    public BatchDeleteCommand toCommand() {
        return new BatchDeleteCommand(issueKeys);
    }
}
