package com.tissue.issuetype.adapter.web.request;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(@NotBlank String name) {

    public RenameIssueTypeCommand toCommand(
            String workspaceKey, String projectKey, Long id, ProjectMemberContext actorContext) {
        return RenameIssueTypeCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .issueTypeId(id)
                .name(Name.of(name))
                .actorContext(actorContext)
                .build();
    }
}
