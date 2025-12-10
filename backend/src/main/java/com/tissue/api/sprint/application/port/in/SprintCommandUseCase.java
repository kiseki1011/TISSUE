package com.tissue.api.sprint.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;
import static com.tissue.api.security.authorization.SprintSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.api.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.api.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.StartSprintCommand;
import com.tissue.api.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.api.sprint.application.dto.response.SprintCommandResult;

@Transactional
public interface SprintCommandUseCase {

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	SprintCommandResult createSprint(CreateSprintCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	SprintCommandResult addIssues(AddSprintIssuesCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_MANAGER)
	SprintCommandResult updateSprint(UpdateSprintCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_MANAGER)
	SprintCommandResult start(StartSprintCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_MANAGER)
	SprintCommandResult complete(CompleteSprintCommand cmd);

	@PreAuthorize(REQUIRES_SPRINT_MANAGER)
	SprintCommandResult migrateIssues(MigrateSprintIssuesCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_MEMBER)
	SprintCommandResult removeIssues(RemoveSprintIssuesCommand cmd);
}
