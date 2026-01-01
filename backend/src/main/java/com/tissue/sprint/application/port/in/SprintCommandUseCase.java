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

    SprintCommandResult addIssues(AddSprintIssuesCommand cmd);

    SprintCommandResult updateSprint(UpdateSprintCommand cmd);

    SprintCommandResult start(StartSprintCommand cmd);

    SprintCommandResult complete(CompleteSprintCommand cmd);

    SprintCommandResult migrateIssues(MigrateSprintIssuesCommand cmd);

    // TODO: should removing issues from a sprint be open to anyone that has ProjectRole.MEMBER?
    SprintCommandResult removeIssues(RemoveSprintIssuesCommand cmd);
}
