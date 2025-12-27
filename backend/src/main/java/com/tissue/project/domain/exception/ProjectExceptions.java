package com.tissue.project.domain.exception;

import static com.tissue.global.exception.ContextKeys.*;
import static com.tissue.project.domain.exception.ProjectErrorCode.*;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.enums.ProjectRole;

public class ProjectExceptions {

	private ProjectExceptions() {
	}

	public static ResourceNotFoundException notFound(Long projectId) {
		return new ResourceNotFoundException(PROJECT_NOT_FOUND)
			.addContext(PROJECT_ID, projectId);
	}

	public static ResourceNotFoundException notFound(String workspaceKey, String projectKey) {
		return new ResourceNotFoundException(PROJECT_NOT_FOUND)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(PROJECT_KEY, projectKey);
	}

	public static ResourceConflictException duplicateKey(String workspaceKey, String projectKey) {
		return new ResourceConflictException(DUPLICATE_PROJECT_KEY)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(PROJECT_KEY, projectKey);
	}

	public static BadRequestException reservedKey(String projectKey) {
		return new BadRequestException(RESERVED_PROJECT_KEY)
			.addContext(PROJECT_KEY, projectKey);
	}

	public static BadRequestException isArchived(Project project) {
		return new BadRequestException(PROJECT_ARCHIVED)
			.addContext(WORKSPACE_KEY, project.getWorkspaceKey())
			.addContext(PROJECT_KEY, project.getKey());
	}

	public static BadRequestException invalidDefaultJoinRole(ProjectRole role) {
		return new BadRequestException(INVALID_DEFAULT_JOIN_ROLE)
			.addContext("defaultJoinRole", role);
	}

	public static ResourceNotFoundException memberNotFound(Project project, Long memberId) {
		return new ResourceNotFoundException(PROJECT_MEMBER_NOT_FOUND)
			.addContext(WORKSPACE_KEY, project.getWorkspaceKey())
			.addContext(PROJECT_KEY, project.getKey())
			.addContext(MEMBER_ID, memberId);
	}

	public static ResourceConflictException memberAlreadyExists(Project project, Long memberId) {
		return new ResourceConflictException(PROJECT_MEMBER_ALREADY_EXISTS)
			.addContext(WORKSPACE_KEY, project.getWorkspaceKey())
			.addContext(PROJECT_KEY, project.getKey())
			.addContext(MEMBER_ID, memberId);
	}

	public static BadRequestException selfKick() {
		return new BadRequestException(SELF_KICK_NOT_ALLOWED);
	}

	public static BadRequestException selfRole() {
		return new BadRequestException(SELF_ROLE_MODIFICATION_NOT_ALLOWED);
	}
}
