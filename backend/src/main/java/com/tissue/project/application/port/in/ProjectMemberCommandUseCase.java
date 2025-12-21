package com.tissue.project.application.port.in;

import static com.tissue.security.authorization.project.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.JoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;

public interface ProjectMemberCommandUseCase {

	// TODO: addMembers, kickMember, join을 ProjectParticipationUseCase로 분리할까?
	@PreAuthorize(REQUIRES_PROJECT_ADMIN)
	ProjectMembersCommandResult addMembers(AddProjectMembersCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ADMIN)
	ProjectMemberCommandResult kickMember(KickProjectMemberCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_JOINABLE)
	ProjectMemberCommandResult join(JoinProjectCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ADMIN)
	ProjectMemberCommandResult changeProjectRole(ChangeProjectRoleCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_VIEWER)
	ProjectMemberCommandResult leave(String workspaceKey, String projectKey, Long meberId);
}
