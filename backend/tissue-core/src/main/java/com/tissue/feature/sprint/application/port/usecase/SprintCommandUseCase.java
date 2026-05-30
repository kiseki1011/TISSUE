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

    void addIssues(Long sprintId, List<String> issueKeys, Long actorMemberId);

    void updateSprint(Long sprintId, UpdateSprintCommand cmd, Long actorMemberId);

    void start(Long sprintId, Instant dueAt, Long actorMemberId);

    void complete(Long sprintId, Long actorMemberId);

    void migrateIssues(Long sprintId, MigrateSprintIssuesCommand cmd, Long actorMemberId);

    void removeIssues(Long sprintId, List<String> issueKeys, Long actorMemberId);

    void cancelSprint(Long sprintId, Long actorMemberId);

    void deleteSprint(Long sprintId, Long actorMemberId);
}
