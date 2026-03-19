package com.tissue.feature.issue.web.request;

import com.tissue.feature.issue.application.dto.request.BatchChangeParentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record BatchChangeParentRequest(
        @NotEmpty @Size(max = 100) Set<String> issueKeys,
        @NotBlank String parentIssueKey) {

    public BatchChangeParentCommand toCommand() {
        return new BatchChangeParentCommand(issueKeys, parentIssueKey);
    }
}
