package com.tissue.issuetype.adapter.in.dto.request;

import com.tissue.common.validator.annotation.size.LabelSize;
import com.tissue.common.vo.Name;
import com.tissue.issuetype.application.dto.request.AddOptionCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record AddOptionRequest(@NotBlank @LabelSize String optionName) {

    public AddOptionCommand toCommand(Long issueTypeId, Long issueFieldId, ProjectMemberContext actorContext) {
        return AddOptionCommand.builder()
                .issueTypeId(issueTypeId)
                .issueFieldId(issueFieldId)
                .name(Name.of(optionName))
                .actorContext(actorContext)
                .build();
    }
}
