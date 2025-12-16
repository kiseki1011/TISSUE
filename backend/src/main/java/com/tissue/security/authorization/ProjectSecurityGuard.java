package com.tissue.security.authorization;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.project.domain.enums.ProjectVisibility;
import com.tissue.project.domain.exception.ProjectNotFoundException;

import lombok.RequiredArgsConstructor;

// TODO: 권한 redis 캐싱
// TODO: 각 메서드 주석 추가(WorkspaceRole.ADMIN을 왜 전부 허용하는 cascade authorization을 사용하는지)
@Component
@RequiredArgsConstructor
public class ProjectSecurityGuard {

	private final ProjectMemberQueryRepository projectMemberRepository;
	private final ProjectQueryRepository projectQueryRepository;
	private final WorkspaceSecurityGuard workspaceSecurityGuard;

	public boolean hasReadPermission(
		String workspaceKey,
		String projectKey,
		Long memberId
	) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.existsByWorkspaceKeyAndProjectKeyAndMemberId(
				workspaceKey, projectKey, memberId
			);
	}

	public boolean isMember(
		String workspaceKey,
		String projectKey,
		Long memberId
	) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
				.map(role -> role.isEqualOrHigherThan(ProjectRole.MEMBER))
				.orElse(false);
	}

	public boolean isAdmin(
		String workspaceKey,
		String projectKey,
		Long memberId
	) {
		return isWorkspaceAdmin(workspaceKey, memberId) ||
			projectMemberRepository.findRoleByKeysAndMemberId(workspaceKey, projectKey, memberId)
				.map(role -> role == ProjectRole.ADMIN)
				.orElse(false);
	}

	public boolean canJoin(
		String workspaceKey,
		String projectKey,
		Long memberId
	) {
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

	public boolean hasProjectAdminPermission(
		String workspaceKey,
		Set<String> targetProjectKeys,
		Long memberId
	) {
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

	public boolean canGrantRole(
		String workspaceKey,
		String projectKey,
		Long memberId,
		ProjectRole grantRole
	) {
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
