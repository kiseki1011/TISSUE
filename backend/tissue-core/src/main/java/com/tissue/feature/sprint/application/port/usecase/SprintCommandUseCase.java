package com.tissue.feature.sprint.application.port.usecase;

import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.shared.dto.ProjectIdentifier;
import java.time.Instant;
import java.util.List;

public interface SprintCommandUseCase {

    SprintCommandResult createSprint(ProjectIdentifier projectIdentifier, CreateSprintCommand cmd, Long actorMemberId);

    void addIssues(ProjectIdentifier projectIdentifier, Long sprintId, List<String> issueKeys, Long actorMemberId);

    void updateSprint(ProjectIdentifier projectIdentifier, Long sprintId, UpdateSprintCommand cmd, Long actorMemberId);

    void start(ProjectIdentifier projectIdentifier, Long sprintId, Instant dueAt, Long actorMemberId);

    void complete(ProjectIdentifier projectIdentifier, Long sprintId, Long actorMemberId);

    void migrateIssues(
            ProjectIdentifier projectIdentifier, Long sprintId, MigrateSprintIssuesCommand cmd, Long actorMemberId);

    void removeIssues(ProjectIdentifier projectIdentifier, Long sprintId, List<String> issueKeys, Long actorMemberId);

    // TODO: delete
}
