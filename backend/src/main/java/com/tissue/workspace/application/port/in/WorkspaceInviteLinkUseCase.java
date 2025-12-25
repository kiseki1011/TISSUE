package com.tissue.workspace.application.port.in;

import static com.tissue.security.authorization.project.ProjectSecurityExpressions.*;
import static com.tissue.security.authorization.workspace.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.request.ExpireLinkCommand;
import com.tissue.workspace.application.dto.request.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.response.WorkspaceMemberCommandResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
	String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ROLE_GRANT_PERMISSION)
	String createProjectLink(CreateProjectInviteLinkCommand cmd);

	@PreAuthorize(REQUIRES_LINK_EDIT_PERMISSION)
	void expireLink(ExpireLinkCommand cmd);

	WorkspaceMemberCommandResponse joinViaLink(JoinViaLinkCommand cmd);

	// TODO: what permission should i set? none?
	WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);
}
