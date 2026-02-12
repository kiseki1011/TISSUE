package com.tissue.feature.sprint.application.port.usecase;

import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.shared.dto.ProjectIdentifier;
import java.time.Instant;
import java.util.List;

public interface SprintCommandUseCase {

    SprintCommandResult createSprint(ProjectIdentifier projectIdentifier, CreateSprintCommand cmd, Long memberId);

    void addIssues(ProjectIdentifier projectIdentifier, Long sprintId, List<String> issueKeys, Long memberId);

    void updateSprint(ProjectIdentifier projectIdentifier, Long sprintId, UpdateSprintCommand cmd, Long memberId);

    void start(ProjectIdentifier projectIdentifier, Long sprintId, Instant dueAt, Long memberId);

    void complete(ProjectIdentifier projectIdentifier, Long sprintId, Long memberId);

    void migrateIssues(
            ProjectIdentifier projectIdentifier, Long sprintId, MigrateSprintIssuesCommand cmd, Long memberId);

    void removeIssues(ProjectIdentifier projectIdentifier, Long sprintId, List<String> issueKeys, Long memberId);

    // TODO: delete
}
