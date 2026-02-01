package com.tissue.sprint.adapter.web.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.sprint.application.dto.request.StartSprintCommand;
import java.time.Instant;

public record StartSprintRequest(Instant dueAt) {

    public StartSprintCommand toCommand(Long sprintId, ProjectMemberContext actorContext) {
        return StartSprintCommand.builder()
                                 .sprintId(sprintId)
                                 .dueAt(dueAt)
                                 .actorContext(actorContext)
                                 .build();
    }
}
