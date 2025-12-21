package com.tissue.workspace.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.security.authorization.SecurityKeyWords;
import com.tissue.security.authorization.project.ProjectSecurityExpressions;
import com.tissue.security.authorization.workspace.WorkspaceSecurityExpressions;
import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.request.ExpireLinkCommand;
import com.tissue.workspace.application.dto.request.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.response.WorkspaceMemberCommandResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

	@PreAuthorize(WorkspaceSecurityExpressions.REQUIRES_WORKSPACE_ADMIN)
	String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

	// TODO: refactor security expression for PreAuthorize
	@PreAuthorize(
		ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER + SecurityKeyWords.AND
			+ ProjectSecurityExpressions.REQUIRES_GRANTABLE_PROJECT_ROLE)
	String createProjectLink(CreateProjectInviteLinkCommand cmd);

	@PreAuthorize(WorkspaceSecurityExpressions.REQUIRES_LINK_CREATOR_OR_WORKSPACE_ADMIN)
	void expireLink(ExpireLinkCommand cmd);

	WorkspaceMemberCommandResponse joinViaLink(JoinViaLinkCommand cmd);

	// TODO: what permission should i set? none?
	WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);
}
