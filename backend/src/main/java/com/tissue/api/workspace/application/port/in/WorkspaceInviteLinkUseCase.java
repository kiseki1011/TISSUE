package com.tissue.api.workspace.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;
import static com.tissue.api.security.authorization.SecurityKeyWords.*;
import static com.tissue.api.security.authorization.WorkspaceSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import com.tissue.api.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.api.workspace.application.dto.request.ExpireLinkCommand;
import com.tissue.api.workspace.application.dto.request.JoinViaLinkCommand;
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResult;
import com.tissue.api.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
	@Transactional
	String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_WRITER + AND + REQUIRES_GRANTABLE_PROJECT_ROLE)
	@Transactional
	String createProjectLink(CreateProjectInviteLinkCommand cmd);

	@PreAuthorize(REQUIRES_LINK_CREATOR_OR_WORKSPACE_ADMIN)
	@Transactional
	void expireLink(ExpireLinkCommand cmd);

	@Transactional
	WorkspaceMemberCommandResult joinViaLink(JoinViaLinkCommand cmd);

	@Transactional(readOnly = true)
	WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);
}
