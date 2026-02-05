package com.tissue.issuetype.adapter.web.request;

import com.tissue.common.enums.ColorType;
import com.tissue.global.vo.Name;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateIssueTypeRequest(
        @NotBlank String name,
        @Nullable @Size(max = 255) String description,
        @NotNull ColorType color,
        @NotNull IssueHierarchy issueHierarchy,
        @NotNull Long workflowId) {

    public CreateIssueTypeCommand toCommand() {
        return CreateIssueTypeCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .issueHierarchy(issueHierarchy)
                .workflowId(workflowId)
                .build();
    }
}
