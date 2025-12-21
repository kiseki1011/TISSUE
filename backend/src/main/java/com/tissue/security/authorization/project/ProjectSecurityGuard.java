package com.tissue.security.authorization.project;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.project.domain.exception.ProjectExceptions;
import com.tissue.security.authorization.workspace.WorkspaceSecurityGuard;

import lombok.RequiredArgsConstructor;

// TODO: should i consider redis caching for permission?
@Component
@RequiredArgsConstructor
public class ProjectSecurityGuard {

	private final ProjectMemberQueryRepository projectMemberRepository;
	private final ProjectQueryRepository projectQueryRepository;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;

	public boolean hasReadPermission(String workspaceKey, String projectKey, Long memberId) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.existsByWorkspaceKeyAndProjectKeyAndMemberId(
				workspaceKey, projectKey, memberId
			);
	}

	public boolean isMember(String workspaceKey, String projectKey, Long memberId) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
				.map(role -> role.isEqualOrHigherThan(ProjectRole.MEMBER))
				.orElse(false);
	}

	public boolean isAdmin(String workspaceKey, String projectKey, Long memberId) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
				.map(role -> role.isEqualOrHigherThan(ProjectRole.ADMIN))
				.orElse(false);
	}

	// TODO: improve name, canJoin -> hasJoinPermission (must change spel expression too!)
	public boolean canJoin(String workspaceKey, String projectKey, Long memberId) {
		if (isNotWorkspaceMember(workspaceKey, memberId)) {
			return false;
		}
		if (workspaceSecurityGuard.isAdmin(workspaceKey, memberId)) {
			return true;
		}

		return projectQueryRepository.findVisibilityByKeys(workspaceKey, projectKey)
			.map(visibility -> visibility == ProjectVisibility.PUBLIC)
			.orElseThrow(() -> ProjectExceptions.notFound(workspaceKey, projectKey));
	}

	// TODO: improve name (must change spel expression too!)
	public boolean hasProjectAdminPermission(String workspaceKey, Set<String> targetProjectKeys, Long memberId) {
		if (targetProjectKeys == null || targetProjectKeys.isEmpty()) {
			return false;
		}

		List<ProjectMember> myAdminProjects = projectMemberRepository.findAllAdminsByKeysAndMemberId(
			workspaceKey,
			targetProjectKeys,
			memberId
		);

		return myAdminProjects.size() == targetProjectKeys.size();
	}

	// TODO: improve name, canGrantRole -> hasRoleGrantPermission (must change spel expression too!)
	public boolean canGrantRole(String workspaceKey, String projectKey, Long memberId, ProjectRole grantRole) {
		if (isWorkspaceAdmin(workspaceKey, memberId)) {
			return true;
		}
		return projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
			.map(actorRole -> actorRole.isEqualOrHigherThan(grantRole))
			.orElse(false);
	}

	private boolean isNotWorkspaceMember(String workspaceKey, Long memberId) {
		return !workspaceSecurityGuard.isMember(workspaceKey, memberId);
	}

	private boolean isWorkspaceAdmin(String workspaceKey, Long memberId) {
		return workspaceSecurityGuard.isAdmin(workspaceKey, memberId);
	}
}
