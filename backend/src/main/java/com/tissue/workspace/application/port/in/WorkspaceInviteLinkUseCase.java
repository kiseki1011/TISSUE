package com.tissue.workspace.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workspace.application.dto.request.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.request.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.request.ExpireLinkCommand;
import com.tissue.workspace.application.dto.request.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.response.WorkspaceMemberCommandResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.security.authorization.ProjectSecurityExpressions;
import com.tissue.security.authorization.SecurityKeyWords;
import com.tissue.security.authorization.WorkspaceSecurityExpressions;

public interface WorkspaceInviteLinkUseCase {

	@Transactional
	@PreAuthorize(WorkspaceSecurityExpressions.REQUIRES_WORKSPACE_ADMIN)
	String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

	@Transactional
	@PreAuthorize(
		ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER + SecurityKeyWords.AND + ProjectSecurityExpressions.REQUIRES_GRANTABLE_PROJECT_ROLE)
	String createProjectLink(CreateProjectInviteLinkCommand cmd);

	@Transactional
	@PreAuthorize(WorkspaceSecurityExpressions.REQUIRES_LINK_CREATOR_OR_WORKSPACE_ADMIN)
	void expireLink(ExpireLinkCommand cmd);

	@Transactional
	WorkspaceMemberCommandResponse joinViaLink(JoinViaLinkCommand cmd);

	@Transactional(readOnly = true)
	WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);
}
