package com.tissue.sprint.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.StartSprintCommand;
import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;

public interface SprintCommandUseCase {

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	SprintCommandResult createSprint(CreateSprintCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	SprintCommandResult addIssues(AddSprintIssuesCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_EDIT_PERMISSION)
	SprintCommandResult updateSprint(UpdateSprintCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_EDIT_PERMISSION)
	SprintCommandResult start(StartSprintCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_EDIT_PERMISSION)
	SprintCommandResult complete(CompleteSprintCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_EDIT_PERMISSION)
	SprintCommandResult migrateIssues(MigrateSprintIssuesCommand cmd);

	// TODO: should removing issues from a sprint be open to anyone that has ProjectRole.MEMBER?
	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	SprintCommandResult removeIssues(RemoveSprintIssuesCommand cmd);
}
