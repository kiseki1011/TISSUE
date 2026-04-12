package com.tissue.feature.sprint.application.port.usecase;

import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.shared.dto.ProjectIdentifier;
import java.time.Instant;
import java.util.List;

public interface SprintCommandUseCase {

    SprintCommandResult createSprint(ProjectIdentifier pid, CreateSprintCommand cmd, Long actorMemberId);

    void addIssues(String workspaceKey, Long sprintId, List<String> issueKeys, Long actorMemberId);

    void updateSprint(String workspaceKey, Long sprintId, UpdateSprintCommand cmd, Long actorMemberId);

    void start(String workspaceKey, Long sprintId, Instant dueAt, Long actorMemberId);

    void complete(String workspaceKey, Long sprintId, Long actorMemberId);

    void migrateIssues(String workspaceKey, Long sprintId, MigrateSprintIssuesCommand cmd, Long actorMemberId);

    void removeIssues(String workspaceKey, Long sprintId, List<String> issueKeys, Long actorMemberId);

    void cancelSprint(String workspaceKey, Long sprintId, Long actorMemberId);

    void deleteSprint(String workspaceKey, Long sprintId, Long actorMemberId);
}
