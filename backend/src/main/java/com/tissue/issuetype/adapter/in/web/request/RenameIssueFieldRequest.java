package com.tissue.issuetype.adapter.in.web.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameIssueFieldCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record RenameIssueFieldRequest(@NotBlank @LabelSize String name) {

    public RenameIssueFieldCommand toCommand(Long issueTypeId, Long issueFieldId, ProjectMemberContext actorContext) {
        return RenameIssueFieldCommand.builder()
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .name(Name.of(name))
                .actorContext(actorContext)
                .build();
    }
}
