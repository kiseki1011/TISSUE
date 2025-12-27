package com.tissue.workspace.application.port.in;

import static com.tissue.project.application.service.authorization.ProjectAuthExpressions.*;
import static com.tissue.workspace.application.service.authorization.WorkspaceAuthExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;

import com.tissue.workspace.application.dto.in.CreateProjectInviteLinkCommand;
import com.tissue.workspace.application.dto.in.CreateWorkspaceInviteLinkCommand;
import com.tissue.workspace.application.dto.in.ExpireLinkCommand;
import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.out.query.WorkspaceInviteLinkDetail;

public interface WorkspaceInviteLinkUseCase {

	@PreAuthorize(REQUIRES_WORKSPACE_ADMIN)
	String createWorkspaceLink(CreateWorkspaceInviteLinkCommand cmd);

	@PreAuthorize(REQUIRES_PROJECT_ROLE_GRANT_PERMISSION)
	String createProjectLink(CreateProjectInviteLinkCommand cmd);

	@PreAuthorize(REQUIRES_LINK_EDIT_PERMISSION)
	void expireLink(ExpireLinkCommand cmd);

	WorkspaceMemberResponse joinViaLink(JoinViaLinkCommand cmd);

	// TODO: getLinkInfo -> getLinkDetail
	@PreAuthorize(REQUIRES_WORKSPACE_MEMBER)
	WorkspaceInviteLinkDetail getLinkInfo(String workspaceKey, String token);

	// TODO: getWorkspaceLinks
	//  all active links for the workspace
}
