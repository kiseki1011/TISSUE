package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issuetype.application.dto.request.CreateIssueTypeCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateIssueTypeRequest(
        @NotBlank @LabelSize String name,
        @Nullable @Size(max = 255) String description,
        @NotNull ColorType color,
        @NotNull IssueHierarchy issueHierarchy,
        @NotNull Long workflowId) {

    public CreateIssueTypeCommand toCommand(String workspaceKey, String projectKey, ProjectMemberContext actorContext) {
        return CreateIssueTypeCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .name(Name.of(name))
                .description(description)
                .color(color)
                .issueHierarchy(issueHierarchy)
                .workflowId(workflowId)
                .actorContext(actorContext)
                .build();
    }
}
