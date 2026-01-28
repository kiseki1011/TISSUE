package com.tissue.issuetype.adapter.in.web.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameIssueTypeCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record RenameIssueTypeRequest(@NotBlank @LabelSize String name) {
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
