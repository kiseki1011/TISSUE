package com.tissue.sprint.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.StartSprintCommand;
import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;
import com.tissue.security.authorization.ProjectSecurityExpressions;
import com.tissue.security.authorization.SprintSecurityExpressions;

@Transactional
public interface SprintCommandUseCase {

	@PreAuthorize(ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER)
	SprintCommandResult createSprint(CreateSprintCommand cmd);

	@PreAuthorize(ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER)
	SprintCommandResult addIssues(AddSprintIssuesCommand cmd);

	@PreAuthorize(SprintSecurityExpressions.REQUIRES_SPRINT_MANAGER)
	SprintCommandResult updateSprint(UpdateSprintCommand cmd);

	@PreAuthorize(SprintSecurityExpressions.REQUIRES_SPRINT_MANAGER)
	SprintCommandResult start(StartSprintCommand cmd);

	@PreAuthorize(SprintSecurityExpressions.REQUIRES_SPRINT_MANAGER)
	SprintCommandResult complete(CompleteSprintCommand cmd);

	@PreAuthorize(SprintSecurityExpressions.REQUIRES_SPRINT_MANAGER)
	SprintCommandResult migrateIssues(MigrateSprintIssuesCommand cmd);

	@PreAuthorize(ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER)
	SprintCommandResult removeIssues(RemoveSprintIssuesCommand cmd);
}
