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
import com.tissue.api.workspace.application.dto.response.WorkspaceMemberCommandResponse;
import com.tissue.api.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

	@Transactional
	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
	String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_PROJECT_MEMBER + AND + REQUIRES_GRANTABLE_PROJECT_ROLE)
	String createProjectLink(CreateProjectInviteLinkCommand cmd);

	@Transactional
	@PreAuthorize(REQUIRES_LINK_CREATOR_OR_WORKSPACE_ADMIN)
	void expireLink(ExpireLinkCommand cmd);

	@Transactional
	WorkspaceMemberCommandResponse joinViaLink(JoinViaLinkCommand cmd);

	@Transactional(readOnly = true)
	WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);
}
