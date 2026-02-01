package com.tissue.issuetype.adapter.web.request;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.application.dto.request.RenameOptionCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotBlank;

public record RenameOptionRequest(@NotBlank String name) {

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
