package com.tissue.issuetype.adapter.web.request;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.AddOptionCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record AddOptionRequest(@NotBlank String optionName) {

    public AddOptionCommand toCommand(Long issueTypeId, Long issueFieldId,
        ProjectMemberContext actorContext) {
        return AddOptionCommand.builder()
                               .issueTypeId(issueTypeId)
                               .issueFieldId(issueFieldId)
                               .name(Name.of(optionName))
                               .actorContext(actorContext)
                               .build();
    }
}
