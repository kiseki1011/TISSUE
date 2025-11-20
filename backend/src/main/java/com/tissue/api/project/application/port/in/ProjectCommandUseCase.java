package com.tissue.api.project.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.project.application.dto.request.CreateProjectCommand;
import com.tissue.api.project.application.dto.request.DeleteProjectCommand;
import com.tissue.api.project.application.dto.request.UpdateProjectCommand;
import com.tissue.api.project.application.dto.request.UpdateProjectKeyCommand;
import com.tissue.api.project.application.dto.response.ProjectCommandResult;
import com.tissue.api.security.authorization.WorkspaceSecurityExpressions;

public interface ProjectCommandUseCase {

	@Transactional
	ProjectCommandResult create(CreateProjectCommand cmd);

	//  TODO: ProjectRole.ADMIN 이상
	@Transactional
	// @PreAuthorize(ProjectSecurityExpressions.REQUIRES_ADMIN)
	ProjectCommandResult update(UpdateProjectCommand cmd);

	@Transactional
	@PreAuthorize(WorkspaceSecurityExpressions.REQUIRES_ADMIN)
	ProjectCommandResult updateKey(UpdateProjectKeyCommand cmd);

	@Transactional
	@PreAuthorize(WorkspaceSecurityExpressions.REQUIRES_ADMIN)
	ProjectCommandResult delete(DeleteProjectCommand cmd);
}
