package com.tissue.feature.sprint.application.port.in;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import java.time.Instant;
import java.util.List;

public interface SprintCommandUseCase {

    SprintCommandResult createSprint(CreateSprintCommand cmd, ProjectMemberContext actorContext);

    void addIssues(Long sprintId, List<String> issueKeys, ProjectMemberContext actorContext);

    void updateSprint(Long sprintId, UpdateSprintCommand cmd, ProjectMemberContext actorContext);

    void start(Long sprintId, Instant dueAt, ProjectMemberContext actorContext);

    void complete(Long sprintId, ProjectMemberContext actorContext);

    void migrateIssues(Long sprintId, MigrateSprintIssuesCommand cmd, ProjectMemberContext actorContext);

    void removeIssues(Long sprintId, List<String> issueKeys, ProjectMemberContext actorContext);

    // TODO: delete
}
