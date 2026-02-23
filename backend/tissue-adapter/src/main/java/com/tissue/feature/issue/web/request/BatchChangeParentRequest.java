package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record BatchChangeParentRequest(
        @NotEmpty Set<String> issueKeys, @NotBlank String parentIssueKey) {

    public BatchChangeParentCommand toCommand() {
        return new BatchChangeParentCommand(issueKeys, parentIssueKey);
    }
}
