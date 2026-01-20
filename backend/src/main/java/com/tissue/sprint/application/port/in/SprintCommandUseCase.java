package com.tissue.sprint.application.port.in;

import com.tissue.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.StartSprintCommand;
import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;

public interface SprintCommandUseCase {

    SprintCommandResult createSprint(CreateSprintCommand cmd);

    void addIssues(AddSprintIssuesCommand cmd);

    void updateSprint(UpdateSprintCommand cmd);

    void start(StartSprintCommand cmd);

    void complete(CompleteSprintCommand cmd);

    void migrateIssues(MigrateSprintIssuesCommand cmd);

    void removeIssues(RemoveSprintIssuesCommand cmd);
}
