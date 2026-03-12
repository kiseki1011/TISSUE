package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.BatchRemoveParentCommand;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record BatchRemoveParentRequest(@NotEmpty Set<String> issueKeys) {

    public BatchRemoveParentCommand toCommand() {
        return new BatchRemoveParentCommand(issueKeys);
    }
}
