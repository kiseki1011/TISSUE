package com.tissue.api.security.authorization;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.enums.ProjectRole;
import com.tissue.api.project.domain.enums.ProjectVisibility;
import com.tissue.api.project.domain.exception.ProjectNotFoundException;
import com.tissue.api.project.domain.port.out.ProjectMemberQueryRepository;
import com.tissue.api.project.domain.port.out.ProjectQueryRepository;

import lombok.RequiredArgsConstructor;

// TODO: 권한 redis 캐싱
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

	public boolean hasWritePermission(String workspaceKey, String projectKey, Long memberId) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
				.map(role -> role.isEqualOrHigherThan(ProjectRole.MEMBER))
				.orElse(false);
	}

	public boolean isAdmin(String workspaceKey, String projectKey, Long memberId) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
				.map(role -> role == ProjectRole.ADMIN)
				.orElse(false);
	}

	public boolean canJoin(String workspaceKey, String projectKey, Long memberId) {
		if (isNotWorkspaceMember(workspaceKey, memberId)) {
			return false;
		}
		if (workspaceSecurityGuard.isAdmin(workspaceKey, memberId)) {
			return true;
		}

		return projectQueryRepository.findVisibilityByKeys(workspaceKey, projectKey)
			.map(visibility -> visibility == ProjectVisibility.PUBLIC)
			.orElseThrow(() -> new ProjectNotFoundException(projectKey, workspaceKey));
	}

	private boolean isNotWorkspaceMember(String workspaceKey, Long memberId) {
		return !workspaceSecurityGuard.isMember(workspaceKey, memberId);
	}

	private boolean isWorkspaceAdmin(String workspaceKey, Long memberId) {
		return workspaceSecurityGuard.isAdmin(workspaceKey, memberId);
	}
}
