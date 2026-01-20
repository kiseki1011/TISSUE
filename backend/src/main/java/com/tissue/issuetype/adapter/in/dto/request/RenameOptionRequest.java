package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameOptionCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(@NotBlank @LabelSize String name) {

    public RenameOptionCommand toCommand(
            Long issueTypeId, Long issueFieldId, Long optionId, ProjectMemberContext actorContext) {
        return RenameOptionCommand.builder()
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .optionId(optionId)
                .name(Name.of(name))
                .actorContext(actorContext)
                .build();
    }
}
