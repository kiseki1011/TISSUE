package com.tissue.security.authorization.project;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authorization.workspace.WorkspaceSecurityGuard;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectSecurityGuard {

	private final ProjectQueryRepository projectQueryRepository;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;

	public boolean isViewer(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.VIEWER);
	}

	public boolean isMember(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.MEMBER);
	}

	public boolean isAdmin(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		return userDetails.hasProjectRole(workspaceKey, projectKey, ProjectRole.ADMIN);
	}

	public boolean canJoinViaDirectAccess(String workspaceKey, String projectKey, MemberUserDetails userDetails) {
		if (workspaceSecurityGuard.isAdmin(workspaceKey, userDetails)) {
			return true;
		}
		if (isNotWorkspaceMember(workspaceKey, userDetails)) {
			return false;
		}
		return isProjectVisibilityPublic(workspaceKey, projectKey);
	}

	public boolean canGrantRole(String workspaceKey, String projectKey, ProjectRole grantRole,
		MemberUserDetails userDetails) {
		if (workspaceSecurityGuard.isAdmin(workspaceKey, userDetails)) {
			return true;
		}
		if (!isViewer(workspaceKey, projectKey, userDetails)) {
			return false;
		}
		return userDetails.hasProjectRole(workspaceKey, projectKey, grantRole);
	}

	private boolean isNotWorkspaceMember(String workspaceKey, MemberUserDetails userDetails) {
		return !workspaceSecurityGuard.isMember(workspaceKey, userDetails);
	}

	private Boolean isProjectVisibilityPublic(String workspaceKey, String projectKey) {
		return projectQueryRepository.findVisibilityByKeys(workspaceKey, projectKey)
			.map(visibility -> visibility == ProjectVisibility.PUBLIC)
			.orElse(false);
	}

}
