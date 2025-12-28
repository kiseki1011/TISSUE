package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.issuetype.application.dto.request.ReorderOptionsCommand;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderOptionsRequest(@NotEmpty List<Long> targetOrderedIds) {
    public ReorderOptionsCommand toCommand(
            String workspaceKey, String projectKey, Long issueTypeId, Long issueFieldId) {
        return ReorderOptionsCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .targetOrderedIds(targetOrderedIds)
                .build();
    }
}
