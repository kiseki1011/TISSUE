package com.tissue.api.project.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.project.application.dto.request.AddProjectMembersCommand;
import com.tissue.api.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.api.project.application.dto.request.JoinProjectCommand;
import com.tissue.api.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.api.project.application.dto.request.LeaveProjectCommand;
import com.tissue.api.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.api.project.application.dto.response.ProjectMembersCommandResult;

@Transactional
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

	@PreAuthorize(REQUIRES_PROJECT_ACCESS)
	ProjectMemberCommandResult leave(LeaveProjectCommand cmd);
}
