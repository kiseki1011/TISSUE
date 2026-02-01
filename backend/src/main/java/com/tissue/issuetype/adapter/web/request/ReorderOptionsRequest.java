package com.tissue.issuetype.adapter.web.request;

import com.tissue.issuetype.application.dto.request.ReorderOptionsCommand;
import com.tissue.project.application.dto.ProjectMemberContext;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderOptionsRequest(@NotEmpty List<Long> targetOrderedIds) {

    public ReorderOptionsCommand toCommand(Long issueTypeId, Long issueFieldId,
        ProjectMemberContext actorContext) {
        return ReorderOptionsCommand.builder()
                                    .issueTypeId(issueTypeId)
                                    .issueFieldId(issueFieldId)
                                    .targetOrderedIds(targetOrderedIds)
                                    .actorContext(actorContext)
                                    .build();
    }
}
